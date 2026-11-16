package com.neuedu.controller;

import com.neuedu.service.AlipayService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付宝支付接口。
 * /alipay/pay 需登录（前端挂号成功后调用，拿收银台跳转 URL）；
 * /alipay/notify 与 /alipay/return 为支付宝服务器/浏览器回调，已在 WebConfig 放行免登录。
 */
@RestController
@RequestMapping("/alipay")
public class AlipayController {

    private static final Logger log = LoggerFactory.getLogger(AlipayController.class);

    @Resource
    private AlipayService alipayService;

    /** 挂号成功后请求收银台跳转地址 */
    @PostMapping("pay")
    Result pay(Long regId) {
        if (regId == null) {
            return Result.fail("缺少挂号记录ID");
        }
        try {
            String url = alipayService.buildPagePayUrl(regId);
            return Result.success(url, "已生成支付地址");
        } catch (Exception e) {
            return Result.fail("生成支付地址失败：" + e.getMessage());
        }
    }

    /** 支付宝异步通知：验签后回纯文本 success，否则支付宝会重试 */
    @PostMapping(value = "notify", produces = "text/plain;charset=UTF-8")
    String notify(HttpServletRequest request) {
        boolean ok = alipayService.verifyNotify(request.getParameterMap());
        if (ok) {
            String tradeStatus = request.getParameter("trade_status");
            String outTradeNo = request.getParameter("out_trade_no");
            log.info("支付宝回调验签通过 out_trade_no={} trade_status={}", outTradeNo, tradeStatus);
            // TRADE_SUCCESS / TRADE_FINISHED 表示支付成功，把挂号单改为待就诊
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                alipayService.markPaidByRegNo(outTradeNo);
            }
            return "success";
        }
        return "failure";
    }

    /** 支付宝同步回跳：浏览器支付完成跳回这里，标记挂号为待就诊并给一个提示页 */
    @GetMapping(value = "return", produces = "text/html;charset=UTF-8")
    String returnUrl(HttpServletRequest request) {
        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        // 本地演示环境收不到异步通知，回跳即视为支付完成，把挂号单改为待就诊
        boolean paid = alipayService.markPaidByRegNo(outTradeNo);
        return "<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"utf-8\"><title>支付结果</title>"
                + "<style>body{font-family:sans-serif;text-align:center;padding-top:80px}"
                + ".ok{color:#2e7d32;font-size:22px;font-weight:700}.no{color:#c62828;font-size:22px;font-weight:700}"
                + ".muted{color:#888;font-size:13px;margin-top:14px}</style></head><body>"
                + (outTradeNo != null
                        ? "<div class=\"ok\">✔ 挂号费支付已完成，已进入待就诊</div>"
                        : "<div class=\"no\">未获取到支付结果</div>")
                + "<p>订单号：" + (outTradeNo == null ? "-" : outTradeNo)
                + "<br/>支付宝交易号：" + (tradeNo == null ? "-" : tradeNo)
                + "<br/>状态更新：" + (paid ? "已改为待就诊" : "无需更新（可能已处理）") + "</p>"
                + "<p class=\"muted\">请关闭本页返回 HIS 继续就诊。</p>"
                + "</body></html>";
    }
}
