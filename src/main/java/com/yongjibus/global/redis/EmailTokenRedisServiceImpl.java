package com.yongjibus.global.redis;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.yongjibus.auth.service.EmailTokenService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailTokenRedisServiceImpl implements EmailTokenService {
    private final StringRedisTemplate redisTemplate;

    private static final long AUTH_CODE_EXPIRATION = 600;
    private static final String AUTH_CODE_PREFIX = "authCode:";

    private static final String VERIFIED_PREFIX = "verified:";

    @Override
    public void setAuthCode(String email, String authCode) {
        String key = getAuthCodeKey(email);
        redisTemplate.opsForValue().set(key, authCode, AUTH_CODE_EXPIRATION, TimeUnit.SECONDS);
    }

    @Override
    public String getAuthCode(String email) {
        String key = getAuthCodeKey(email);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void setVerified(String email) {
        String key = getVerifiedKey(email);
        redisTemplate.opsForValue().set(key, "true");
    }

    @Override
    public boolean isVerified(String email) {
        String key = getVerifiedKey(email);
        String value = redisTemplate.opsForValue().get(key);
        return "true".equals(value);
    }

    @Override
    public void deleteAuthCode(String email) {
        String key = getAuthCodeKey(email);
        redisTemplate.delete(key);
    }

    private String getAuthCodeKey(String email) {
        return AUTH_CODE_PREFIX + email;
    }

    private String getVerifiedKey(String email) {
        return VERIFIED_PREFIX + email;
    }
}
