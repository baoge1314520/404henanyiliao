package com.neuedu.chat;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 登录令牌管理：token 存 Redis，2 小时有效，访问时续期。
 * 管理端与患者端使用不同前缀，互不串用。
 */
@Component
public class TokenService {

    private static final String PREFIX = "login:token:";
    private static final String PATIENT_PREFIX = "patient:token:";
    private static final Duration TTL = Duration.ofHours(2);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String create(Long userId) {
        return create(PREFIX, userId);
    }

    public Long resolve(String token) {
        return resolve(PREFIX, token);
    }

    public void delete(String token) {
        delete(PREFIX, token);
    }

    /** 患者端令牌：与管理员令牌隔离，避免患者 token 被当成 userId 使用 */
    public String createPatient(Long patientId) {
        return create(PATIENT_PREFIX, patientId);
    }

    public Long resolvePatient(String token) {
        return resolve(PATIENT_PREFIX, token);
    }

    public void deletePatient(String token) {
        delete(PATIENT_PREFIX, token);
    }

    private String create(String prefix, Long id) {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(prefix + token, String.valueOf(id), TTL);
        return token;
    }

    private Long resolve(String prefix, String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String key = prefix + token;
        String id = stringRedisTemplate.opsForValue().get(key);
        if (id == null) {
            return null;
        }
        stringRedisTemplate.expire(key, TTL);
        return Long.valueOf(id);
    }

    private void delete(String prefix, String token) {
        if (StringUtils.isNotBlank(token)) {
            stringRedisTemplate.delete(prefix + token);
        }
    }
}