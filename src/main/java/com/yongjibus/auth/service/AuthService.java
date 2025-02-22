package com.yongjibus.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.auth.domain.Member;
import com.yongjibus.auth.repository.AuthRepository;
import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;
import com.yongjibus.global.redis.EmailTokenRedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional
@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
    private final AuthRepository authRepository;
    private final EmailService emailService;
    private final EmailTokenRedisService emailTokenRedisService;

    /**
     * 이메일 인증 코드를 생성하고 발송합니다.
     * 
     * @param email 인증 코드를 받을 이메일 주소
     */
    public void sendAuthEmail(String email) {
        String authCode = AuthCodeGenerator.generateCode();
        log.info("인증 코드 : {}", authCode);

        emailTokenRedisService.setAuthCode(email, authCode);
        //emailService.sendAuthEmail(email, authCode);
    }

    /**
     * 사용자가 입력한 인증 코드의 유효성을 검증합니다.
     * 
     * @param email 검증할 이메일 주소
     * @param authCode 사용자가 입력한 인증 코드
     * @return 인증 성공 여부
     */
    public void verifyAuthCode(String email, String authCode) {
        String storedAuthCode = emailTokenRedisService.getAuthCode(email);
        log.info("인증 이메일 : {}, 인증 코드 : {}", email, storedAuthCode);

        if (storedAuthCode == null) {
            throw new AuthException(ErrorCode.INVALID_AUTH_CODE);
        }

        if (authCode.equals(storedAuthCode)) {
            emailTokenRedisService.deleteAuthCode(email);
            emailTokenRedisService.setVerified(email);
        } else {
            throw new AuthException(ErrorCode.INVALID_AUTH_CODE);
        }
    }
    
    public void login(String email, String password) {
        Member member = authRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

        if (!member.getPassword().equals(password)) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * 새로운 회원을 등록합니다.
     * 이메일 인증, 이메일 중복, 사용자명 중복을 검증합니다.
     * 
     * @param member 등록할 회원 정보
     */
    public void signup(Member member) {
        validateEmailVerification(member.getEmail());
        validateDuplicateEmail(member.getEmail());
        validateDuplicateUsername(member.getUsername());

        authRepository.save(member);
    }

    private void validateEmailVerification(String email) {
        if (!emailTokenRedisService.isVerified(email)) {
            throw new AuthException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (authRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateDuplicateUsername(String username) {
        if (authRepository.existsByUsername(username)) {
            throw new AuthException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
    }
}
