package com.yongjibus.auth.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.auth.email.EmailPendingRepository;
import com.yongjibus.auth.email.EmailTokenService;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.infra.email.EmailService;
import com.yongjibus.global.infra.jwt.JwtService;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuthService {
    private final MemberService memberService;
    private final EmailService emailService;
    private final EmailTokenService emailTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final EmailPendingRepository emailPendingRepository;
    /**
     * 이메일 인증 코드를 생성하고 발송합니다.
     * 
     * @param email 인증 코드를 받을 이메일 주소
     */
    @Transactional
    public String sendAuthEmail(String email) {
        String authCode = AuthCodeGenerator.generateCode();

        emailTokenService.setAuthCode(email, authCode);
        emailService.sendAuthEmail(email, authCode);
        // 발송 요청한 이메일 저장 (이메일 발송 요청 추적)
        emailPendingRepository.saveEmailPending(email);
        return authCode;
    }

    /**
     * 사용자가 입력한 인증 코드의 유효성을 검증합니다.
     * 
     * @param email 검증할 이메일 주소
     * @param authCode 사용자가 입력한 인증 코드
     * @return 인증 성공 여부
     */
    @Transactional
    public void verifyAuthCode(String email, String authCode) {
        String storedAuthCode = emailTokenService.getAuthCode(email);

        if (storedAuthCode == null) {
            throw new AuthException(ErrorCode.INVALID_AUTH_CODE);
        }

        if (authCode.equals(storedAuthCode)) {
            emailTokenService.deleteAuthCode(email);
            emailTokenService.setVerified(email);
            // 발송 요청한 이메일 삭제 (이메일 발송 요청 추적)
            emailPendingRepository.deleteEmailPending(email);
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
     * @return 발급된 AccessToken과 RefreshToken의 리스트 [accessToken, refreshToken]
     * @throws AuthException 이메일이 존재하지 않거나 비밀번호가 일치하지 않을 경우
     */
    @Transactional
    public List<String> login(String email, String password) {

        Member member = memberService.getMemberByEmail(email);

        if(member.getIsDeleted()) {
            throw new AuthException(ErrorCode.MEMBER_DELETED);
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 토큰 발급
        String accessToken = jwtService.createAccessToken(email);
        String refreshToken = jwtService.createAndSaveRefreshToken(email);
        
        return List.of(accessToken, refreshToken);
    }

    /**
     * 새로운 회원을 등록합니다.
     * 이메일 인증, 이메일 중복, 사용자명 중복을 검증합니다.
     * 
     * @param member 등록할 회원 정보
     */
    @Transactional
    public List<String> signup(Member member) {
        validateEmailVerification(member.getEmail());
        memberService.validateMemberInfoToSignup(member);

        Member newMember = Member.builder()
            .email(member.getEmail())
            .name(member.getName())
            .password(passwordEncoder.encode(member.getPassword()))
            .username(member.getUsername())
            .build();

        memberService.saveMember(newMember);

        String accessToken = jwtService.createAccessToken(member.getEmail());
        String refreshToken = jwtService.createAndSaveRefreshToken(member.getEmail());

        return List.of(accessToken, refreshToken);
    }

    /**
     * RefreshToken을 사용하여 AccessToken을 재발행합니다.
     * 
     * @param refreshToken 사용자의 RefreshToken
     * @return 새로 발급된 AccessToken과 RefreshToken의 리스트 [accessToken, refreshToken]
     * @throws AuthException RefreshToken이 유효하지 않을 경우
     */
    @Transactional
    public List<String> refreshAccessToken(String refreshToken, Member member) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        String email = member.getEmail();
        String newAccessToken = jwtService.createAccessToken(email);
        String newRefreshToken = jwtService.createAndSaveRefreshToken(email);
        
        return List.of(newAccessToken, newRefreshToken);
    }

    /**
     * 사용자 로그아웃을 처리합니다.
     * RefreshToken을 무효화하여 로그아웃 처리합니다.
     *
     * @param member 로그아웃할 회원
     */
    @Transactional
    public void logout(Member member) {
        // Redis에서 RefreshToken 삭제
        jwtService.deleteRefreshToken(member.getEmail());
    }

    /**
     * 회원 탈퇴를 처리합니다.
     * 회원 상태를 삭제됨으로 변경하고 저장합니다.
     *
     * @param member 탈퇴할 회원
     */
    @Transactional
    public void signoutMember(Member member) {
        // RefreshToken 삭제 (로그아웃 처리)
        jwtService.deleteRefreshToken(member.getEmail());
        
        // 회원 상태를 삭제됨으로 변경
        member.delete();
        
        // 변경된 회원 정보 저장
        memberService.saveMember(member);
    }
    
    /**
     * 사용자 이름(username) 중복 여부를 확인합니다.
     * 
     * @param username 확인할 사용자 이름
     * @return 중복되면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    public boolean checkUsernameExists(String username) {
        return memberService.checkUsernameExists(username);
    }

    private void validateEmailVerification(String email) {
        if (!emailTokenService.isVerified(email)) {
            throw new AuthException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }
}
