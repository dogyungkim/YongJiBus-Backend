package com.yongjibus.auth.email;

import java.math.BigInteger;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.Message;
import com.yongjibus.global.infra.gmail.GmailApiService;

@Service
@RequiredArgsConstructor
public class EmailBounceService {
    private final EmailPendingRepository emailPendingRepository;

    private final GmailApiService gmailApiService;

    public void processBounceNotification(String historyId) {
        try {
            var history = gmailApiService.getHistory(BigInteger.valueOf(Long.parseLong(historyId)));
            if (history.getHistory() == null) return;

            for (History h : history.getHistory()) {
                // 새로운 메시지가 추가된 경우만 처리
                if (h.getMessagesAdded() != null) {
                    //Message ID 추출
                    for(HistoryMessageAdded hma : h.getMessagesAdded()) {
                        String messageId = hma.getMessage().getId();
                        // Message ID를 사용하여 메시지를 가져옴
                        var message = gmailApiService.getMessage(messageId);
                        // 메시지를 파싱하여 실패한 이메일 주소 정보를 추출
                        String failedEmailAddress = getFailedEmailAddressFromBounceMail(message);

                        if (failedEmailAddress != null && emailPendingRepository.isEmailPending(failedEmailAddress)) {
                            // 이메일이 전송 실패한 경우

                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFailedEmailAddressFromBounceMail(Message message) {
        return message.getPayload().getHeaders().stream()
            .filter(h -> "X-Failed-Recipients".equalsIgnoreCase(h.getName()))
            .map(h -> h.getValue()) // 여러 주소면 콤마로 구분됨
            .findFirst()
            .orElse(null);
    }
}
