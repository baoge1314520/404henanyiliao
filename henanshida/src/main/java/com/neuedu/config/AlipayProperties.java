package com.neuedu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝沙箱/开放平台配置，对应 application.yml 的 alipay.* 段。
 * 密钥为账号专属，请在支付宝开放平台「研发-沙箱」里复制后填入 yml，切勿提交到公开仓库。
 */
@Data
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /** 网关地址：沙箱 https://openapi.alipaydev.com/gateway.do；正式 https://openapi.alipay.com/gateway.do */
    private String gateway;

    /** 沙箱应用 APPID */
    private String appId;

    /** 应用私钥（RSA2，PKCS#8 base64 字符串，不含首尾 PEM 标记） */
    private String privateKey;

    /** 支付宝公钥（用于验签回调，注意不是应用公钥） */
    private String alipayPublicKey;

    /** 异步通知地址（支付宝服务器回调，需公网可达且免登录放行；本地演示可留空） */
    private String notifyUrl;

    /** 同步跳转地址（支付完成后浏览器回跳，免登录放行） */
    private String returnUrl;

    /** 签名类型 */
    private String signType = "RSA2";

    /** 编码 */
    private String charset = "UTF-8";
}
