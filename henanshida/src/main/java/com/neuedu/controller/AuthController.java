package com.neuedu.controller;

import com.mybatisflex.core.query.QueryWrapper;
import com.neuedu.chat.TokenService;
import com.neuedu.core.ResultCode;
import com.neuedu.entity.SysUser;
import com.neuedu.service.SysUserService;
import com.neuedu.vo.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录认证 控制层。账号支持手机号 / 邮箱 / 工号。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private TokenService tokenService;

    @PostMapping("login")
    Result login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return Result.fail("请输入账号和密码");
        }
        String u = username.trim();
        // 账号支持手机号 / 邮箱 / 工号，逐个精确匹配（避免 MyBatis-Flex and/or 重载歧义）
        SysUser user = sysUserService.getOne(QueryWrapper.create().eq("enabled", true).eq("phone", u));
        if (user == null) {
            user = sysUserService.getOne(QueryWrapper.create().eq("enabled", true).eq("email", u));
        }
        if (user == null) {
            user = sysUserService.getOne(QueryWrapper.create().eq("enabled", true).eq("employee_no", u));
        }
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail("账号或密码错误");
        }
        String token = tokenService.create(user.getId());
        SysUser touch = SysUser.builder().id(user.getId()).loginTime(LocalDateTime.now()).build();
        sysUserService.updateById(touch);
        user.setPassword(null);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("token", token);
        content.put("user", user);
        return Result.success(content, "登录成功");
    }

    @PostMapping("register")
    Result register(String realName, String phone, String password, String confirm) {
        if (StringUtils.isBlank(realName) || StringUtils.isBlank(phone) || StringUtils.isBlank(password)) {
            return Result.fail("请填写姓名、手机号和密码");
        }
        String p = phone.trim();
        if (!p.matches("^1[3-9]\\d{9}$")) {
            return Result.fail("手机号格式不正确");
        }
        if (password.length() < 6 || password.length() > 32) {
            return Result.fail("密码长度需为 6-32 位");
        }
        if (StringUtils.isBlank(confirm) || !confirm.equals(password)) {
            return Result.fail("两次输入的密码不一致");
        }
        if (!sysUserService.check("phone", p, null)) {
            return Result.fail("该手机号已注册，请直接登录");
        }
        long now = System.currentTimeMillis();
        SysUser user = SysUser.builder()
                .realName(realName.trim())
                .phone(p)
                .employeeNo("U" + now)
                .gender("0")
                .enabled(true)
                .createDate(new java.sql.Date(now))
                .createTime(new java.sql.Time(now))
                .updateDate(new java.sql.Date(now))
                .updateTime(new java.sql.Time(now))
                .build();
        if (!sysUserService.add(user, password)) {
            return Result.fail("注册失败，请稍后重试");
        }
        // 注册成功即自动登录，直接发令牌
        String token = tokenService.create(user.getId());
        user.setPassword(null);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("token", token);
        content.put("user", user);
        return Result.success(content, "注册成功");
    }

    @GetMapping("info")
    Result info(HttpServletRequest request) {
        Long userId = tokenService.resolve(readToken(request));
        if (userId == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.getInstance(ResultCode.UNAUTHORIZED, null, "未登录或登录已过期");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("logout")
    Result logout(HttpServletRequest request) {
        tokenService.delete(readToken(request));
        return Result.success(null, "已退出登录");
    }

    private String readToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("token");
        }
        return token;
    }
}