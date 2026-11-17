package com.neuedu.config;

import com.neuedu.chat.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录守卫：除 /auth/login 外，所有接口都要带有效 token。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if ("/auth/login".equals(uri)) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("token");
        }
        Long userId = tokenService.resolve(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
            return true;
        }
        // 患者端令牌：与管理端隔离，只标记 patientId，患者端接口据此校验归属
        Long patientId = tokenService.resolvePatient(token);
        if (patientId != null) {
            request.setAttribute("patientId", patientId);
            return true;
        }
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"content\":null,\"message\":\"未登录或登录已过期\"}");
        return false;
    }
}