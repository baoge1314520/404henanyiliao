package com.neuedu.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.mybatisflex.core.query.QueryWrapper;
import com.neuedu.config.AlipayProperties;
import com.neuedu.entity.SysPatient;
import com.neuedu.entity.SysRegistration;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝电脑网站支付：把挂号单转成收银台跳转地址，并处理异步回调验签。
 */
@Service
public class AlipayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayService.class);

    @Resource
    private AlipayClient alipayClient;
    @Resource
    private AlipayProperties props;
    @Resource
    private SysRegistrationService sysRegistrationService;
    @Resource
    private SysPatientService sysPatientService;

    /**
     * 生成 alipay.trade.page.pay 收银台跳转 URL（GET 方式，前端直接 window.open/location 跳转）。
     * out_trade_no 用挂号单号，total_amount 用挂号费快照。
     */
    public String buildPagePayUrl(Long regId) {
        SysRegistration reg = sysRegistrationService.getById(regId);
        if (reg == null) {
            throw new IllegalArgumentException("挂号记录不存在: " + regId);
        }

        SysPatient patient = reg.getPatientId() != null ? sysPatientService.getById(reg.getPatientId()) : null;
        String patientName = patient != null && StringUtils.isNotBlank(patient.getPatientName())
                ? patient.getPatientName() : "患者";

        BigDecimal fee = reg.getRegFee();
        // 支付宝要求金额大于 0；职称缺失导致 0 元时用 0.01 兜底，保证演示可跳转
        String amount = (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0)
                ? "0.01" : fee.toPlainString();

        // 手动拼 bizContent：subject 不含引号，out_trade_no 为字母数字，无需转义
        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                reg.getRegNo(), amount, "河南师大HIS门诊挂号费-" + patientName);

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setBizContent(bizContent);
        if (StringUtils.isNotBlank(props.getReturnUrl())) {
            request.setReturnUrl(props.getReturnUrl());
        }
        if (StringUtils.isNotBlank(props.getNotifyUrl())) {
            request.setNotifyUrl(props.getNotifyUrl());
        }

        try {
            // 第二个参数 "GET"：让 SDK 返回拼好签名的完整网关 URL，而不是自动提交表单
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "GET");
            if (!response.isSuccess()) {
                log.warn("支付宝下单失败 code={} subMsg={}", response.getCode(), response.getSubMsg());
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("生成支付宝收银台地址异常", e);
            throw new RuntimeException("生成支付地址失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步通知验签。校验通过返回 true，业务侧再据 trade_status 更新订单。
     */
    public boolean verifyNotify(Map<String, String[]> parameterMap) {
        Map<String, String> params = new HashMap<>();
        parameterMap.forEach((k, v) -> params.put(k, v != null && v.length > 0 ? v[0] : ""));
        try {
            boolean ok = AlipaySignature.rsaCheckV1(
                    params, props.getAlipayPublicKey(), props.getCharset(), props.getSignType());
            if (!ok) {
                log.warn("支付宝回调验签失败 out_trade_no={}", params.get("out_trade_no"));
            }
            return ok;
        } catch (Exception e) {
            log.error("支付宝回调验签异常", e);
            return false;
        }
    }

    /**
     * 支付成功后把挂号单从"待支付"(0) 改为"待就诊"(1)。
     * 仅对当前处于待支付的单子生效，避免重复回调或已取消单被误改。
     *
     * @param outTradeNo 商户订单号，即挂号单号 reg_no
     * @return 是否更新成功
     */
    public boolean markPaidByRegNo(String outTradeNo) {
        if (StringUtils.isBlank(outTradeNo)) {
            return false;
        }
        SysRegistration reg = sysRegistrationService.getOne(
                QueryWrapper.create().eq("reg_no", outTradeNo.trim()));
        if (reg == null) {
            log.warn("支付回调找不到挂号单 out_trade_no={}", outTradeNo);
            return false;
        }
        if (!"0".equals(reg.getStatus())) {
            log.info("挂号单 {} 当前状态={} 非待支付，跳过", outTradeNo, reg.getStatus());
            return false;
        }
        SysRegistration update = SysRegistration.builder().id(reg.getId()).status("1").build();
        boolean ok = sysRegistrationService.updateById(update);
        log.info("挂号单 {} 支付成功，状态改为待就诊 ok={}", outTradeNo, ok);
        return ok;
    }
}
