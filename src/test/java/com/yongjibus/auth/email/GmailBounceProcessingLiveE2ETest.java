package com.yongjibus.auth.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.yongjibus.global.infra.gmail.GmailApiService;
import com.yongjibus.global.infra.gmail.GmailPubSubTokenVerifier;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "GMAIL_E2E_ENABLED", matches = "true")
class GmailBounceProcessingLiveE2ETest {

    private static final String AUTHORIZATION = "Bearer live-test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GmailApiService gmailApiService;

    @Autowired
    private EmailBounceService emailBounceService;

    @Autowired
    private EmailPendingRepository emailPendingRepository;

    @Autowired
    private GmailHistoryCheckpointRepository checkpointRepository;

    @MockBean
    private GmailPubSubTokenVerifier tokenVerifier;

    private String processedEmail;

    @DynamicPropertySource
    static void gmailProperties(DynamicPropertyRegistry registry) {
        registry.add("gmail.enabled", () -> true);
        registry.add("gmail.oauth.client-id", () -> requiredEnv("GMAIL_OAUTH_CLIENT_ID"));
        registry.add("gmail.oauth.client-secret", () -> requiredEnv("GMAIL_OAUTH_CLIENT_SECRET"));
        registry.add("gmail.oauth.refresh-token", () -> requiredEnv("GMAIL_OAUTH_REFRESH_TOKEN"));
    }

    @BeforeEach
    void setUp() {
        checkpointRepository.deleteAll();
        when(tokenVerifier.isValid(AUTHORIZATION)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        checkpointRepository.deleteAll();
        if (processedEmail != null) {
            emailPendingRepository.deleteEmailPending(processedEmail);
        }
    }

    @Test
    void realGmailBounceIsProcessedThroughNotificationEndpoint() throws Exception {
        BigInteger currentHistoryId = gmailApiService.getCurrentHistoryId();
        Message bounceMessage = findBounceMessage();
        assertThat(bounceMessage)
                .as("a message with X-Failed-Recipients under the configured Gmail label")
                .isNotNull();

        processedEmail = failedRecipient(bounceMessage);
        assertThat(processedEmail)
                .as("the selected Gmail message's failed recipient")
                .isNotBlank();

        emailBounceService.saveEmailPending(processedEmail);
        checkpointRepository.save(GmailHistoryCheckpoint.initialize(BigInteger.ONE));

        mockMvc.perform(post("/gmail/bounce")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(pubSubEnvelope(currentHistoryId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertThat(emailPendingRepository.isEmailPending(processedEmail)).isTrue();
        GmailHistoryCheckpoint savedCheckpoint = checkpointRepository
                .findById(GmailHistoryCheckpoint.CHECKPOINT_KEY)
                .orElseThrow();
        assertThat(savedCheckpoint.getLastHistoryId()).isGreaterThanOrEqualTo(currentHistoryId);
    }

    private Message findBounceMessage() throws Exception {
        String pageToken = null;
        do {
            var response = gmailApiService.getBounceMessages(pageToken);
            if (response.getMessages() != null) {
                for (Message message : response.getMessages()) {
                    Message fullMessage = gmailApiService.getMessage(message.getId());
                    if (failedRecipient(fullMessage) != null) {
                        return fullMessage;
                    }
                }
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());
        return null;
    }

    private String failedRecipient(Message message) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return null;
        }
        return message.getPayload().getHeaders().stream()
                .filter(header -> "X-Failed-Recipients".equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(null);
    }

    private String pubSubEnvelope(String historyId) {
        String payload = "{\"historyId\":\"" + historyId + "\"}";
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "{\"message\":{\"data\":\"" + encoded + "\"}}";
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for the live Gmail E2E test");
        }
        return value;
    }
}
