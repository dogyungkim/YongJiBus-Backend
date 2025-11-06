package com.yongjibus.auth.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Component
public class InMemoryEmailTokenService implements EmailTokenService {

    // 인증 코드 캐시 (기본 만료 시간: 5분)
    private final Cache<String, String> authCodeCache;
    
    // 인증 상태 캐시 (기본 만료 시간: 10분)
    private final Cache<String, Boolean> verifiedCache;

    public InMemoryEmailTokenService(
            @Value("${auth.email.code.expiry:5}") int codeExpiryMinutes,
            @Value("${auth.email.verified.expiry:10}") int verifiedExpiryMinutes) {
        
        // 인증 코드 캐시 설정
        this.authCodeCache = Caffeine.newBuilder()
                .expireAfterWrite(codeExpiryMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats()
                .build();
        
        // 인증 상태 캐시 설정
        this.verifiedCache = Caffeine.newBuilder()
                .expireAfterWrite(verifiedExpiryMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats()
                .build();
    }

    @Override
    public void setAuthCode(String email, String authCode) {
        authCodeCache.put(email, authCode);
    }

    @Override
    public String getAuthCode(String email) {
        return authCodeCache.getIfPresent(email);
    }

    @Override
    public void setVerified(String email) {
        verifiedCache.put(email, true);
    }

    @Override
    public boolean isVerified(String email) {
        Boolean verified = verifiedCache.getIfPresent(email);
        return verified != null && verified;
    }

    @Override
    public void deleteAuthCode(String email) {
        authCodeCache.invalidate(email);
    }

}
