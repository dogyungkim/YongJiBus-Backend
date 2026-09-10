package com.yongjibus.global.infra.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

@ExtendWith(MockitoExtension.class)
class GmailPubSubTokenVerifierTest {
    private static final String AUDIENCE = "https://pubsub.example.com/gmail-bounce";
    private static final String SERVICE_ACCOUNT = "pubsub-push@yongji-bus.iam.gserviceaccount.com";

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Mock
    private GoogleIdToken idToken;

    @Mock
    private GoogleIdToken.Payload payload;

    private GmailProperties gmailProperties;
    private GmailPubSubTokenVerifier tokenVerifier;

    @BeforeEach
    void setUp() {
        gmailProperties = new GmailProperties();
        gmailProperties.setPubsubAudience(AUDIENCE);
        gmailProperties.setPubsubServiceAccountEmail(SERVICE_ACCOUNT);
        tokenVerifier = new GmailPubSubTokenVerifier(gmailProperties);
        ReflectionTestUtils.setField(tokenVerifier, "verifier", googleIdTokenVerifier);
    }

    @Test
    @DisplayName("email_verified인 예상 Pub/Sub 서비스 계정 token만 통과한다")
    void isValid_WhenTokenClaimsMatch_ShouldReturnTrue() throws Exception {
        when(googleIdTokenVerifier.verify("token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getEmail()).thenReturn(SERVICE_ACCOUNT);

        assertThat(tokenVerifier.isValid("Bearer token")).isTrue();
    }

    @Test
    @DisplayName("예상 서비스 계정이 아니면 거부한다")
    void isValid_WhenServiceAccountDoesNotMatch_ShouldReturnFalse() throws Exception {
        when(googleIdTokenVerifier.verify("token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(true);
        when(payload.getEmail()).thenReturn("other@example.com");

        assertThat(tokenVerifier.isValid("Bearer token")).isFalse();
    }

    @Test
    @DisplayName("email_verified가 true가 아니면 거부한다")
    void isValid_WhenEmailIsNotVerified_ShouldReturnFalse() throws Exception {
        when(googleIdTokenVerifier.verify("token")).thenReturn(idToken);
        when(idToken.getPayload()).thenReturn(payload);
        when(payload.getEmailVerified()).thenReturn(false);

        assertThat(tokenVerifier.isValid("Bearer token")).isFalse();
    }

    @Test
    @DisplayName("audience 또는 서비스 계정 설정이 없으면 검증하지 않고 거부한다")
    void isValid_WhenConfigurationIsMissing_ShouldReturnFalse() {
        gmailProperties.setPubsubAudience("");

        assertThat(tokenVerifier.isValid("Bearer token")).isFalse();
        verifyNoInteractions(googleIdTokenVerifier);
    }

    @Test
    @DisplayName("Google token 검증 예외는 거부로 처리한다")
    void isValid_WhenTokenVerificationFails_ShouldReturnFalse() throws Exception {
        when(googleIdTokenVerifier.verify("token")).thenThrow(new GeneralSecurityException("invalid"));

        assertThat(tokenVerifier.isValid("Bearer token")).isFalse();
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 검증하지 않고 거부한다")
    void isValid_WhenAuthorizationFormatIsInvalid_ShouldReturnFalse() {
        assertThat(tokenVerifier.isValid(null)).isFalse();
        assertThat(tokenVerifier.isValid("Basic token")).isFalse();
        assertThat(tokenVerifier.isValid("Bearer ")).isFalse();
        verifyNoInteractions(googleIdTokenVerifier);
    }
}
