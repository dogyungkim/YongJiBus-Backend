package com.yongjibus.auth.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.auth.domain.Member;
import com.yongjibus.auth.domain.dto.AuthTokenDTO;
import com.yongjibus.auth.repository.MemberRepository;
import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;
import com.yongjibus.global.jwt.JwtService;
import com.yongjibus.global.redis.EmailTokenRedisService;
import com.yongjibus.global.redis.JwtRedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional
@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
    private final MemberRepository authRepository;
    private final EmailService emailService;
    private final EmailTokenRedisService emailTokenRedisService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtRedisService jwtRedisService;

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
    
    /**
     * 사용자 로그인을 처리합니다.
     * 이메일과 비밀번호를 검증하여 유효한 사용자인지 확인합니다.
     * 
     * @param email 로그인할 사용자의 이메일
     * @param password 로그인할 사용자의 비밀번호
     * @return 발급된 AccessToken과 RefreshToken
     * @throws AuthException 이메일이 존재하지 않거나 비밀번호가 일치하지 않을 경우
     */
    public AuthTokenDTO login(String email, String password) {
        Member member = authRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 토큰 발급
        String accessToken = jwtService.createAccessToken(email);
        String refreshToken = jwtService.createAndSaveRefreshToken(email);
        
        return new AuthTokenDTO(accessToken, refreshToken);
    }

    /**
     * 새로운 회원을 등록합니다.
     * 이메일 인증, 이메일 중복, 사용자명 중복을 검증합니다.
     * 
     * @param member 등록할 회원 정보
     */
    public List<String> signup(Member member) {
        validateEmailVerification(member.getEmail());
        validateDuplicateEmail(member.getEmail());
        validateDuplicateUsername(member.getUsername());

        Member newMember = Member.builder()
            .email(member.getEmail())
            .name(member.getName())
            .password(passwordEncoder.encode(member.getPassword()))
            .username(member.getUsername())
            .build();

        authRepository.save(newMember);

        String accessToken = jwtService.createAccessToken(member.getEmail());
        String refreshToken = jwtService.createAndSaveRefreshToken(member.getEmail());

        return List.of(accessToken, refreshToken);
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

    /**
     * RefreshToken을 사용하여 AccessToken을 재발행합니다.
     * 
     * @param refreshToken 사용자의 RefreshToken
     * @return 새로 발급된 AccessToken
     * @throws AuthException RefreshToken이 유효하지 않을 경우
     */
    public AuthTokenDTO refreshAccessToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (jwtService.isTokenExpired(refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        String email = jwtService.getEmailFromToken(refreshToken);
        String newAccessToken = jwtService.createAccessToken(email);
        
        return new AuthTokenDTO(newAccessToken, null);
    }

    /**
     * 사용자 로그아웃을 처리합니다.
     * RefreshToken을 무효화하여 로그아웃 처리합니다.
     * 
     * @param refreshToken 무효화할 RefreshToken
     * @throws AuthException RefreshToken이 유효하지 않을 경우
     */
    public void logout(Member member) {

        // Redis에서 RefreshToken 삭제
        jwtRedisService.deleteRefreshToken(member.getEmail());

        member.delete();
        
        authRepository.save(member);
    }
}
