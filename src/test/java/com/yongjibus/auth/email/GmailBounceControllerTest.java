package com.yongjibus.auth.email;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yongjibus.global.infra.gmail.GmailPubSubTokenVerifier;
import com.yongjibus.global.error.handler.GlobalExceptionHandler;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class GmailBounceControllerTest extends ControllerTestSupport {

    @Mock
    private EmailBounceService emailBounceService;

    @Mock
    private GmailPubSubTokenVerifier tokenVerifier;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GmailBounceController(emailBounceService, objectMapper, tokenVerifier))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("Authorization이 없으면 401을 반환하고 바운스 처리를 호출하지 않는다")
    void post_WhenAuthorizationIsMissing_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/gmail/bounce")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("not-json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(tokenVerifier).isValid(null);
        verifyNoInteractions(emailBounceService);
    }

    @Test
    @DisplayName("Bearer가 아닌 Authorization이면 401을 반환하고 바운스 처리를 호출하지 않는다")
    void post_WhenAuthorizationIsNotBearer_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/gmail/bounce")
                        .header("Authorization", "Basic credentials")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("not-json"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(tokenVerifier).isValid("Basic credentials");
        verifyNoInteractions(emailBounceService);
    }

    @Test
    @DisplayName("유효한 Pub/Sub 인증과 envelope이면 바운스 처리를 호출한다")
    void post_WhenAuthorizationAndEnvelopeAreValid_ShouldProcessBounce() throws Exception {
        when(tokenVerifier.isValid("Bearer valid-token")).thenReturn(true);

        mockMvc.perform(post("/gmail/bounce")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(pubSubEnvelope("105")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        verify(emailBounceService).processBounceNotification("105");
    }

    private String pubSubEnvelope(String historyId) {
        String payload = "{\"historyId\":\"" + historyId + "\"}";
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "{\"message\":{\"data\":\"" + encoded + "\"}}";
    }
}
