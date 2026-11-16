package com.neuedu.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 支付宝异步通知(服务器回调)与同步回跳(浏览器)不带登录 token，必须免卡放行
                .excludePathPatterns("/auth/login", "/auth/register", "/alipay/notify", "/alipay/return");
    }
}