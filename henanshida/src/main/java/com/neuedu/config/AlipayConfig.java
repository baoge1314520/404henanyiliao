package com.neuedu.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 装配支付宝客户端。密钥未填时不创建 Bean，避免本地没配沙箱就启动失败；
 * 填好 application.yml 的 alipay.* 后重启后端即生效。
 */
@Configuration
@EnableConfigurationProperties(AlipayProperties.class)
public class AlipayConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties p) {
        return new DefaultAlipayClient(
                p.getGateway(),
                p.getAppId(),
                p.getPrivateKey(),
                "json",
                p.getCharset(),
                p.getAlipayPublicKey(),
                p.getSignType()
        );
    }
}
