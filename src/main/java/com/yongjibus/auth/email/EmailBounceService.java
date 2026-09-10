package com.yongjibus.auth.email;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.Message;
import com.yongjibus.global.infra.gmail.GmailApiService;    

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailBounceService {
    private final EmailPendingRepository emailPendingRepository;
    private final GmailApiService gmailApiService;
    private final GmailHistoryCheckpointRepository gmailHistoryCheckpointRepository;

    @Transactional
    public void processBounceNotification(String historyId) {
        BigInteger notifiedHistoryId = new BigInteger(historyId);
        Optional<GmailHistoryCheckpoint> checkpointOptional = gmailHistoryCheckpointRepository
                .findById(GmailHistoryCheckpoint.CHECKPOINT_KEY);

        if (checkpointOptional.isEmpty()) {
            log.warn("No Gmail history checkpoint found. Initializing checkpoint with notification historyId={}", notifiedHistoryId);
            initializeCheckpointIfAbsent(notifiedHistoryId);
            return;
        }

        GmailHistoryCheckpoint checkpoint = checkpointOptional.get();
        if (notifiedHistoryId.compareTo(checkpoint.getLastHistoryId()) <= 0) {
            log.info("Skipping duplicate or stale Gmail notification. notifiedHistoryId={}, checkpoint={}",
                    notifiedHistoryId, checkpoint.getLastHistoryId());
            return;
        }

        HistorySyncResult historySyncResult;
        try {
            historySyncResult = getHistorySince(checkpoint.getLastHistoryId());
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() != 404) {
                throw new RuntimeException(e);
            }
            recoverFromExpiredHistory(checkpoint);
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            for (History h : historySyncResult.histories()) {
                // 새로운 메시지가 추가된 경우만 처리
                if (h.getMessagesAdded() != null) {
                    for (HistoryMessageAdded hma : h.getMessagesAdded()) {
                        processBounceMessage(hma.getMessage().getId());
                    }
                } else {
                    log.debug("MessagesAdded is null");
                }
            }
            checkpoint.advanceTo(historySyncResult.latestHistoryId());
            gmailHistoryCheckpointRepository.save(checkpoint);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void initializeCheckpointIfAbsent(BigInteger historyId) {
        gmailHistoryCheckpointRepository.findById(GmailHistoryCheckpoint.CHECKPOINT_KEY)
                .orElseGet(() -> gmailHistoryCheckpointRepository.save(GmailHistoryCheckpoint.initialize(historyId)));
    }

    private String getFailedEmailAddressFromBounceMail(Message message) {
        return message.getPayload().getHeaders().stream()
            .filter(h -> "X-Failed-Recipients".equalsIgnoreCase(h.getName()))
            .map(h -> h.getValue()) // 여러 주소면 콤마로 구분됨
            .findFirst()
            .orElse(null);
    }

    private void processBounceMessage(String messageId) throws IOException {
        var message = gmailApiService.getMessage(messageId);
        String failedEmailAddress = getFailedEmailAddressFromBounceMail(message);
        if (failedEmailAddress != null) {
            emailPendingRepository.setEmailPendingStatus(failedEmailAddress, true);
        }
    }

    private void recoverFromExpiredHistory(GmailHistoryCheckpoint checkpoint) {
        try {
            BigInteger currentHistoryId = gmailApiService.getCurrentHistoryId();
            String pageToken = null;
            do {
                var response = gmailApiService.getBounceMessages(pageToken);
                if (response.getMessages() != null) {
                    for (Message message : response.getMessages()) {
                        processBounceMessage(message.getId());
                    }
                }
                pageToken = response.getNextPageToken();
            } while (pageToken != null && !pageToken.isBlank());

            checkpoint.advanceTo(currentHistoryId);
            gmailHistoryCheckpointRepository.save(checkpoint);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void saveEmailPending(String emailAddress) {
        emailPendingRepository.saveEmailPending(emailAddress);
    }

    @Transactional
    public void setEmailPendingStatus(String emailAddress, boolean isPending) {
        emailPendingRepository.setEmailPendingStatus(emailAddress, isPending);
    }

    @Transactional(readOnly = true)
    public boolean isEmailPending(String emailAddress) {
        return emailPendingRepository.isEmailPending(emailAddress);
    }

    @Transactional
    public void deleteEmailPending(String emailAddress) {
        emailPendingRepository.deleteEmailPending(emailAddress);
    }

    private HistorySyncResult getHistorySince(BigInteger startHistoryId) throws IOException {
        List<History> histories = new ArrayList<>();
        BigInteger latestHistoryId = startHistoryId;
        String pageToken = null;

        do {
            var response = gmailApiService.getHistory(startHistoryId, pageToken);
            if (response.getHistory() != null) {
                histories.addAll(response.getHistory());
            }
            if (response.getHistoryId() != null) {
                latestHistoryId = latestHistoryId.max(response.getHistoryId());
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());

        return new HistorySyncResult(histories, latestHistoryId);
    }

    private record HistorySyncResult(List<History> histories, BigInteger latestHistoryId) {
    }
}
