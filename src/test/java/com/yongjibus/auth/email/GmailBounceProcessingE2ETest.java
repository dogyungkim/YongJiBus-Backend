package com.yongjibus.auth.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.yongjibus.global.infra.gmail.GmailApiService;
import com.yongjibus.global.infra.gmail.GmailPubSubTokenVerifier;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GmailBounceProcessingE2ETest {

    private static final String EMAIL = "bounce@example.com";
    private static final String MESSAGE_ID = "bounce-message-1";
    private static final String AUTHORIZATION = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailBounceService emailBounceService;

    @Autowired
    private EmailPendingRepository emailPendingRepository;

    @Autowired
    private GmailHistoryCheckpointRepository checkpointRepository;

    @MockBean
    private GmailApiService gmailApiService;

    @MockBean
    private GmailPubSubTokenVerifier tokenVerifier;

    @BeforeEach
    void setUp() {
        checkpointRepository.deleteAll();
        emailPendingRepository.deleteEmailPending(EMAIL);
        emailBounceService.saveEmailPending(EMAIL);
        checkpointRepository.save(GmailHistoryCheckpoint.initialize(BigInteger.valueOf(100)));
        when(tokenVerifier.isValid(AUTHORIZATION)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        checkpointRepository.deleteAll();
        emailPendingRepository.deleteEmailPending(EMAIL);
    }

    @Test
    void pubSubNotificationProcessesBounceAndExposesPendingState() throws Exception {
        when(gmailApiService.getHistory(BigInteger.valueOf(100), null))
                .thenReturn(new ListHistoryResponse()
                        .setHistoryId(BigInteger.valueOf(105))
                        .setHistory(List.of(new History()
                                .setMessagesAdded(List.of(new HistoryMessageAdded()
                                        .setMessage(new Message().setId(MESSAGE_ID)))))));
        when(gmailApiService.getMessage(MESSAGE_ID)).thenReturn(bounceMessage());

        mockMvc.perform(post("/gmail/bounce")
                        .header("Authorization", AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(pubSubEnvelope("105")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        assertThat(emailPendingRepository.isEmailPending(EMAIL)).isTrue();
        assertThat(checkpointRepository.findById(GmailHistoryCheckpoint.CHECKPOINT_KEY))
                .get()
                .extracting(GmailHistoryCheckpoint::getLastHistoryId)
                .isEqualTo(BigInteger.valueOf(105));

        mockMvc.perform(get("/gmail/bounce").param("emailAddress", EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(gmailApiService).getHistory(BigInteger.valueOf(100), null);
        verify(gmailApiService).getMessage(MESSAGE_ID);
    }

    private Message bounceMessage() {
        return new Message().setPayload(new MessagePart().setHeaders(List.of(
                new MessagePartHeader()
                        .setName("X-Failed-Recipients")
                        .setValue(EMAIL))));
    }

    private String pubSubEnvelope(String historyId) {
        String payload = "{\"historyId\":\"" + historyId + "\"}";
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "{\"message\":{\"data\":\"" + encoded + "\"}}";
    }
}
