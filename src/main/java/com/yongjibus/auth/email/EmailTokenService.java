package com.yongjibus.auth.email;

public interface EmailTokenService {
    void setAuthCode(String email, String authCode);
    String getAuthCode(String email);
    void setVerified(String email);
    boolean isVerified(String email);
    void deleteAuthCode(String email);
}