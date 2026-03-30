package com.yongjibus.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.yongjibus.global.infra.email.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportEmailDeliveryService {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(ReportCreatedEvent event) {
        try {
            emailService.sendReportEmail(
                event.reportedUsername(),
                event.reason(),
                event.reporterUsername(),
                event.roomId()
            );
        } catch (Exception e) {
            log.warn("신고 알림 메일 전송 실패. reportedUser={}, reporter={}, roomId={}",
                event.reportedUsername(),
                event.reporterUsername(),
                event.roomId(),
                e);
        }
    }
}
