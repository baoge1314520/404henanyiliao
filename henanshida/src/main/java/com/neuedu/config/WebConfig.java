package com.neuedu.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Resource
    private UploadStorage uploadStorage;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 支付宝异步通知(服务器回调)与同步回跳(浏览器)不带登录 token，必须免卡放行；
                // /uploads/** 是头像等静态图片，<img> 加载不带 token，同样放行。
                .excludePathPatterns("/auth/login", "/auth/register", "/patientAuth/login", "/patientAuth/register",
                        "/alipay/notify", "/alipay/return", "/uploads/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把 /uploads/** 映射到本地上传目录，存进去的头像即可通过完整 URL 访问
        String location = uploadStorage.getRoot().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}