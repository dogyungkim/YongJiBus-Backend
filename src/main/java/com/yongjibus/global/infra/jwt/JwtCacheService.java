package com.yongjibus.global.infra.jwt;

public interface JwtCacheService {
    void setRefreshToken(String email, String refreshToken);
    String getRefreshToken(String email);
    void deleteRefreshToken(String email);
}