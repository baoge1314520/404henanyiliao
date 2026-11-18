package com.neuedu.config;

import com.neuedu.chat.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录守卫 + 角色隔离：
 * 管理端令牌(login:token:) / 医生端令牌(doctor:token:) / 患者端令牌(patient:token:) 三者互不串用。
 * /doctorAuth/** 只认医生令牌，/patientAuth/** 只认患者令牌，其余管理端接口只认管理员令牌；
 * 科室/职称全量只读、支付宝、AI 问答为跨角色共用接口，任意登录身份即可。
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
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("token");
        }
        Long userId = tokenService.resolve(token);
        Long doctorId = userId != null ? null : tokenService.resolveDoctor(token);
        Long patientId = (userId != null || doctorId != null) ? null : tokenService.resolvePatient(token);
        if (userId != null) {
            request.setAttribute("userId", userId);
        }
        if (doctorId != null) {
            request.setAttribute("doctorId", doctorId);
        }
        if (patientId != null) {
            request.setAttribute("patientId", patientId);
        }
        if (userId == null && doctorId == null && patientId == null) {
            return reject(response, "未登录或登录已过期");
        }
        if (uri.startsWith("/doctorAuth/")) {
            return doctorId != null ? true : reject(response, "请使用医生账号登录医生端");
        }
        if (uri.startsWith("/patientAuth/")) {
            return patientId != null ? true : reject(response, "请使用患者账号登录患者端");
        }
        // 跨角色共用：患者挂号大厅要读科室，支付与 AI 问答三端都用
        if (uri.equals("/sysDept/listAll") || uri.equals("/sysTitle/listAll")
                || uri.startsWith("/alipay/") || uri.startsWith("/ai/")) {
            return true;
        }
        return userId != null ? true : reject(response, "无权限：请使用管理员账号登录管理端");
    }

    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"content\":null,\"message\":\"" + message + "\"}");
        return false;
    }
}