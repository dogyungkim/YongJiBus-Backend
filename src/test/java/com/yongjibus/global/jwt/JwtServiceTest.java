package com.yongjibus.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.yongjibus.global.infra.jwt.JwtRepository;
import com.yongjibus.global.infra.jwt.JwtService;

import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private JwtRepository jwtRepository;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_SECRET_KEY = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
    private final Long TEST_ACCESS_TOKEN_EXPIRATION = 86400000L; // 1일
    private final Long TEST_REFRESH_TOKEN_EXPIRATION = 432000000L; // 5일

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationPeriod", TEST_ACCESS_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationPeriod", TEST_REFRESH_TOKEN_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "key", Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes()));
    }

    @Test
    @DisplayName("액세스 토큰 생성 테스트")
    void createAccessTokenTest() {
        // when
        String accessToken = jwtService.createAccessToken(TEST_EMAIL);

        // then
        assertThat(accessToken).isNotNull();
        assertThat(jwtService.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtService.getEmailFromToken(accessToken)).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 및 저장 테스트")
    void createAndSaveRefreshTokenTest() {
        // when
        String refreshToken = jwtService.createAndSaveRefreshToken(TEST_EMAIL);
        when(jwtRepository.getRefreshToken(TEST_EMAIL)).thenReturn(refreshToken);

        // then
        assertThat(refreshToken).isNotNull();
        assertThat(jwtService.validateRefreshToken(refreshToken)).isTrue();
        verify(jwtRepository).setRefreshToken(TEST_EMAIL, refreshToken);
    }

    @Test
    @DisplayName("리프레시 토큰 검증 성공 테스트")
    void validateRefreshTokenSuccessTest() {
        // given
        String refreshToken = jwtService.createAndSaveRefreshToken(TEST_EMAIL);

        // when
        when(jwtRepository.getRefreshToken(TEST_EMAIL)).thenReturn(refreshToken);

        // then
        assertThat(jwtService.validateRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰 검증 실패 테스트 - 저장된 토큰 없음")
    void validateRefreshTokenFailNoStoredTokenTest() {
        // given
        String refreshToken = "valid-refresh-token";

        // when & then
        assertThat(jwtService.validateRefreshToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 검증 실패 테스트 - 토큰 불일치")
    void validateRefreshTokenFailTokenMismatchTest() {
        // given
        String refreshToken = jwtService.createAndSaveRefreshToken(TEST_EMAIL);
        String storedToken = "different-refresh-token";
        when(jwtRepository.getRefreshToken(TEST_EMAIL)).thenReturn(storedToken);

        // when & then
        assertThat(jwtService.validateRefreshToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("리프레시 토큰 갱신 테스트")
    void rotateRefreshTokenTest() {
        // when
        String rotatedToken = jwtService.rotateRefreshToken(TEST_EMAIL);

        // then
        verify(jwtRepository).deleteRefreshToken(TEST_EMAIL);
        verify(jwtRepository).setRefreshToken(TEST_EMAIL, rotatedToken);
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 테스트")
    void getEmailFromTokenTest() {
        // given
        String accessToken = jwtService.createAccessToken(TEST_EMAIL);

        // when
        String email = jwtService.getEmailFromToken(accessToken);

        // then
        assertThat(email).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("HTTP 요청에서 토큰 추출 성공 테스트")
    void extractTokenSuccessTest() {
        // given
        String token = "valid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        // when
        Optional<String> extractedToken = jwtService.extractToken(request);

        // then
        assertThat(extractedToken).isPresent();
        assertThat(extractedToken.get()).isEqualTo(token);
    }

    @Test
    @DisplayName("HTTP 요청에서 토큰 추출 실패 테스트 - 헤더 없음")
    void extractTokenFailNoHeaderTest() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        Optional<String> extractedToken = jwtService.extractToken(request);

        // then
        assertThat(extractedToken).isEmpty();
    }

    @Test
    @DisplayName("HTTP 요청에서 토큰 추출 실패 테스트 - Bearer 접두사 없음")
    void extractTokenFailNoBearerPrefixTest() {
        // given
        String token = "valid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);

        // when
        Optional<String> extractedToken = jwtService.extractToken(request);

        // then
        assertThat(extractedToken).isEmpty();
    }
} 