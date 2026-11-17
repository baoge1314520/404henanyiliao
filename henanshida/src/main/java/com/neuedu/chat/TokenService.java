package com.neuedu.chat;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 登录令牌管理：token 存 Redis，2 小时有效，访问时续期。
 */
@Component
public class TokenService {

    private static final String PREFIX = "login:token:";
    private static final Duration TTL = Duration.ofHours(2);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String create(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(PREFIX + token, String.valueOf(userId), TTL);
        return token;
    }

    public Long resolve(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String key = PREFIX + token;
        String userId = stringRedisTemplate.opsForValue().get(key);
        if (userId == null) {
            return null;
        }
        stringRedisTemplate.expire(key, TTL);
        return Long.valueOf(userId);
    }

    public void delete(String token) {
        if (StringUtils.isNotBlank(token)) {
            stringRedisTemplate.delete(PREFIX + token);
        }
    }
}