package com.yongjibus.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yongjibus.auth.controller.dto.AuthTokenDTO;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.infra.email.EmailService;
import com.yongjibus.global.infra.jwt.JwtService;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;
import com.yongjibus.member.service.MemberService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository authRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTokenService emailTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MemberService memberService;

    private Member testMember;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_USERNAME = "testuser";
    private final String TEST_NAME = "테스트유저";
    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .username(TEST_USERNAME)
                .name(TEST_NAME)
                .build();
    }

    @Test
    @DisplayName("이메일 인증 코드 발송 테스트")
    void sendAuthEmailTest() {
        // when
        authService.sendAuthEmail(TEST_EMAIL);

        // then
        verify(emailTokenService).setAuthCode(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 성공 테스트")
    void verifyAuthCodeSuccessTest() {
        // given
        String authCode = "123456";
        when(emailTokenService.getAuthCode(TEST_EMAIL)).thenReturn(authCode);

        // when
        authService.verifyAuthCode(TEST_EMAIL, authCode);

        // then
        verify(emailTokenService).deleteAuthCode(TEST_EMAIL);
        verify(emailTokenService).setVerified(TEST_EMAIL);
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 실패 테스트 - 잘못된 코드")
    void verifyAuthCodeFailTest() {
        // given
        String authCode = "123456";
        String wrongCode = "654321";
        when(emailTokenService.getAuthCode(TEST_EMAIL)).thenReturn(authCode);

        // when & then
        assertThatThrownBy(() -> authService.verifyAuthCode(TEST_EMAIL, wrongCode))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AUTH_CODE);
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest() {
        // given
        when(passwordEncoder.matches(TEST_PASSWORD, testMember.getPassword())).thenReturn(true);
        when(memberService.getMemberByEmail(TEST_EMAIL)).thenReturn(testMember);
        when(jwtService.createAccessToken(anyString())).thenReturn("accessToken");
        when(jwtService.createAndSaveRefreshToken(anyString())).thenReturn("refreshToken");
        // when
        authService.login(TEST_EMAIL, TEST_PASSWORD);

        // then
        verify(memberService).getMemberByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, testMember.getPassword());
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupSuccessTest() {
        // given
        when(emailTokenService.isVerified(TEST_EMAIL)).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(jwtService.createAccessToken(anyString())).thenReturn("accessToken");
        when(jwtService.createAndSaveRefreshToken(anyString())).thenReturn("refreshToken");

        // when
        authService.signup(testMember);

        // then
        verify(memberService).saveMember(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 미인증")
    void signupFailEmailNotVerifiedTest() {
        // given
        when(emailTokenService.isVerified(TEST_EMAIL)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signup(testMember))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 성공 테스트")
    void refreshAccessTokenSuccessTest() {
        // given
        String refreshToken = "validRefreshToken";
        when(jwtService.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.createAccessToken(TEST_EMAIL)).thenReturn("newAccessToken");
        when(jwtService.createAndSaveRefreshToken(TEST_EMAIL)).thenReturn("newRefreshToken");

        // when
        List<String> tokens = authService.refreshAccessToken(refreshToken, testMember);

        // then
        verify(jwtService).validateRefreshToken(refreshToken);
        verify(jwtService).createAccessToken(TEST_EMAIL);
        verify(jwtService).createAndSaveRefreshToken(TEST_EMAIL);
        assertThat(tokens.get(0)).isEqualTo("newAccessToken");
        assertThat(tokens.get(1)).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("액세스 토큰 갱신 실패 테스트 - 유효하지 않은 리프레시 토큰")
    void refreshAccessTokenFailInvalidTokenTest() {
        // given
        String invalidRefreshToken = "invalidRefreshToken";
        when(jwtService.validateRefreshToken(invalidRefreshToken)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(invalidRefreshToken, testMember))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logoutSuccessTest() {
        // when
        authService.logout(testMember);

        // then
        verify(jwtService).deleteRefreshToken(TEST_EMAIL);
        // Member의 delete 메서드가 호출되었는지 확인하기 어려우므로 상태 변경 확인
        // ReflectionTestUtils를 사용하여 private 필드 확인 가능
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 비밀번호")
    void loginFailInvalidPasswordTest() {
        // given
        String wrongPassword = "wrongPassword";
        when(memberService.getMemberByEmail(TEST_EMAIL)).thenReturn(testMember);
        when(passwordEncoder.matches(wrongPassword, testMember.getPassword())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(TEST_EMAIL, wrongPassword))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("회원가입 시 비밀번호 암호화 테스트")
    void signupPasswordEncodingTest() {
        // given
        when(emailTokenService.isVerified(TEST_EMAIL)).thenReturn(true);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        when(jwtService.createAccessToken(anyString())).thenReturn("accessToken");
        when(jwtService.createAndSaveRefreshToken(anyString())).thenReturn("refreshToken");

        // when
        authService.signup(testMember);

        // then
        verify(passwordEncoder).encode(TEST_PASSWORD);
        // Member 객체가 저장될 때 암호화된 비밀번호를 가지고 있는지 확인
        verify(memberService).saveMember(argThat(member -> 
            "encodedPassword".equals(member.getPassword())
        ));
    }
}