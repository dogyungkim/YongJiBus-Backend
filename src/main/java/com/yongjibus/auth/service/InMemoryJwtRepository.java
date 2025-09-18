package com.yongjibus.auth.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.yongjibus.global.infra.jwt.JwtRepository;

@Component
public class InMemoryJwtRepository implements JwtRepository {

    private final Map<String, String> refreshTokenMap = new ConcurrentHashMap<>();

    @Override
    public void setRefreshToken(String email, String refreshToken) {
        refreshTokenMap.put(email, refreshToken);
    }

    @Override
    public String getRefreshToken(String email) {
        return refreshTokenMap.get(email);
    }

    @Override
    public void deleteRefreshToken(String email) {
        refreshTokenMap.remove(email);
    }
}
