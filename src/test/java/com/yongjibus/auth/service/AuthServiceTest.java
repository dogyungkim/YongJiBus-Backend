package com.yongjibus.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yongjibus.auth.domain.Member;
import com.yongjibus.auth.repository.AuthRepository;
import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;
import com.yongjibus.global.redis.EmailTokenRedisService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailTokenRedisService emailTokenRedisService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Member testMember;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .username(TEST_USERNAME)
                .build();
    }

    @Test
    @DisplayName("이메일 인증 코드 발송 테스트")
    void sendAuthEmailTest() {
        // when
        authService.sendAuthEmail(TEST_EMAIL);

        // then
        verify(emailTokenRedisService).setAuthCode(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 성공 테스트")
    void verifyAuthCodeSuccessTest() {
        // given
        String authCode = "123456";
        when(emailTokenRedisService.getAuthCode(TEST_EMAIL)).thenReturn(authCode);

        // when
        authService.verifyAuthCode(TEST_EMAIL, authCode);

        // then
        verify(emailTokenRedisService).deleteAuthCode(TEST_EMAIL);
        verify(emailTokenRedisService).setVerified(TEST_EMAIL);
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 실패 테스트 - 잘못된 코드")
    void verifyAuthCodeFailTest() {
        // given
        String authCode = "123456";
        String wrongCode = "654321";
        when(emailTokenRedisService.getAuthCode(TEST_EMAIL)).thenReturn(authCode);

        // when & then
        assertThatThrownBy(() -> authService.verifyAuthCode(TEST_EMAIL, wrongCode))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_AUTH_CODE);
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest() {
        // given
        when(authRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testMember));
        when(passwordEncoder.matches(TEST_PASSWORD, testMember.getPassword())).thenReturn(true);

        // when
        authService.login(TEST_EMAIL, TEST_PASSWORD);

        // then
        verify(authRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, testMember.getPassword());
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupSuccessTest() {
        // given
        when(emailTokenRedisService.isVerified(TEST_EMAIL)).thenReturn(true);
        when(authRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(authRepository.existsByUsername(TEST_USERNAME)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        // when
        authService.signup(testMember);

        // then
        verify(authRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 미인증")
    void signupFailEmailNotVerifiedTest() {
        // given
        when(emailTokenRedisService.isVerified(TEST_EMAIL)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signup(testMember))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 중복 이메일")
    void signupFailDuplicateEmailTest() {
        // given
        when(emailTokenRedisService.isVerified(TEST_EMAIL)).thenReturn(true);
        when(authRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(testMember))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 중복 사용자명")
    void signupFailDuplicateUsernameTest() {
        // given
        when(emailTokenRedisService.isVerified(TEST_EMAIL)).thenReturn(true);
        when(authRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(authRepository.existsByUsername(TEST_USERNAME)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(testMember))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}