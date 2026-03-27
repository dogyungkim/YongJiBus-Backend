package com.yongjibus.auth.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.yongjibus.global.infra.gmail.GmailApiService;

@ExtendWith(MockitoExtension.class)
class EmailBounceServiceTest {

    @Mock
    private EmailPendingRepository emailPendingRepository;

    @Mock
    private GmailApiService gmailApiService;

    @Mock
    private GmailHistoryCheckpointRepository gmailHistoryCheckpointRepository;

    @InjectMocks
    private EmailBounceService emailBounceService;

    @Test
    @DisplayName("알림에 담긴 historyId가 아니라 마지막 checkpoint부터 history를 조회한다")
    void processBounceNotification_ShouldUseStoredCheckpoint() throws Exception {
        // given
        GmailHistoryCheckpoint checkpoint = GmailHistoryCheckpoint.initialize(BigInteger.valueOf(100));
        when(gmailHistoryCheckpointRepository.findById(GmailHistoryCheckpoint.CHECKPOINT_KEY))
                .thenReturn(Optional.of(checkpoint));

        Message historyMessage = new Message().setId("message-1");
        History history = new History().setMessagesAdded(List.of(new HistoryMessageAdded().setMessage(historyMessage)));
        ListHistoryResponse response = new ListHistoryResponse()
                .setHistory(List.of(history))
                .setHistoryId(BigInteger.valueOf(105));

        when(gmailApiService.getHistory(BigInteger.valueOf(100), null)).thenReturn(response);
        when(gmailApiService.getMessage("message-1")).thenReturn(buildBounceMessage("bounce@example.com"));
        when(emailPendingRepository.isEmailPending("bounce@example.com")).thenReturn(true);

        // when
        emailBounceService.processBounceNotification("105");

        // then
        verify(gmailApiService).getHistory(BigInteger.valueOf(100), null);
        verify(emailPendingRepository).setEmailPendingStatus("bounce@example.com", true);

        ArgumentCaptor<GmailHistoryCheckpoint> checkpointCaptor = ArgumentCaptor.forClass(GmailHistoryCheckpoint.class);
        verify(gmailHistoryCheckpointRepository).save(checkpointCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(checkpointCaptor.getValue().getLastHistoryId())
                .isEqualTo(BigInteger.valueOf(105));
    }

    @Test
    @DisplayName("checkpoint가 없으면 알림 historyId를 기준점으로만 저장하고 잘못된 조회는 하지 않는다")
    void processBounceNotification_WhenCheckpointMissing_ShouldInitializeAndSkip() {
        // given
        when(gmailHistoryCheckpointRepository.findById(GmailHistoryCheckpoint.CHECKPOINT_KEY))
                .thenReturn(Optional.empty());

        // when
        emailBounceService.processBounceNotification("105");

        // then
        verifyNoInteractions(gmailApiService);
        ArgumentCaptor<GmailHistoryCheckpoint> checkpointCaptor = ArgumentCaptor.forClass(GmailHistoryCheckpoint.class);
        verify(gmailHistoryCheckpointRepository).save(checkpointCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(checkpointCaptor.getValue().getLastHistoryId())
                .isEqualTo(BigInteger.valueOf(105));
    }

    @Test
    @DisplayName("중복되거나 더 오래된 알림이면 재조회하지 않는다")
    void processBounceNotification_WhenNotificationIsDuplicate_ShouldSkip() throws Exception {
        // given
        GmailHistoryCheckpoint checkpoint = GmailHistoryCheckpoint.initialize(BigInteger.valueOf(105));
        when(gmailHistoryCheckpointRepository.findById(GmailHistoryCheckpoint.CHECKPOINT_KEY))
                .thenReturn(Optional.of(checkpoint));

        // when
        emailBounceService.processBounceNotification("105");

        // then
        verify(gmailHistoryCheckpointRepository, never()).save(any());
        verifyNoInteractions(gmailApiService);
    }

    @Test
    @DisplayName("watch 등록 시 받은 historyId로 checkpoint를 최초 한 번만 초기화한다")
    void initializeCheckpointIfAbsent_ShouldSaveOnlyWhenMissing() {
        // given
        when(gmailHistoryCheckpointRepository.findById(GmailHistoryCheckpoint.CHECKPOINT_KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(GmailHistoryCheckpoint.initialize(BigInteger.valueOf(200))));

        // when
        emailBounceService.initializeCheckpointIfAbsent(BigInteger.valueOf(200));
        emailBounceService.initializeCheckpointIfAbsent(BigInteger.valueOf(300));

        // then
        ArgumentCaptor<GmailHistoryCheckpoint> checkpointCaptor = ArgumentCaptor.forClass(GmailHistoryCheckpoint.class);
        verify(gmailHistoryCheckpointRepository).save(checkpointCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(checkpointCaptor.getValue().getLastHistoryId())
                .isEqualTo(BigInteger.valueOf(200));
    }

    private Message buildBounceMessage(String failedRecipient) {
        MessagePartHeader header = new MessagePartHeader()
                .setName("X-Failed-Recipients")
                .setValue(failedRecipient);
        MessagePart payload = new MessagePart().setHeaders(List.of(header));
        return new Message().setPayload(payload);
    }
}
