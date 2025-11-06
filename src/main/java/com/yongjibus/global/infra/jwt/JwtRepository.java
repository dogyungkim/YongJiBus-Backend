package com.yongjibus.global.infra.jwt;

public interface JwtRepository {
    void setRefreshToken(String email, String refreshToken);
    String getRefreshToken(String email);
    void deleteRefreshToken(String email);
}