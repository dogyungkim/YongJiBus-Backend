package com.yongjibus.member.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.infra.email.EmailService;

@ExtendWith(MockitoExtension.class)
class ReportEmailDeliveryServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReportEmailDeliveryService reportEmailDeliveryService;

    @Test
    @DisplayName("신고 메일 이벤트를 받으면 메일 전송을 수행한다")
    void deliver_ShouldSendReportEmail() {
        // given
        ReportCreatedEvent event = new ReportCreatedEvent("reported-user", "욕설", "reporter-user", 11L);

        // when
        reportEmailDeliveryService.deliver(event);

        // then
        verify(emailService).sendReportEmail("reported-user", "욕설", "reporter-user", 11L);
    }

    @Test
    @DisplayName("신고 메일 전송 실패는 로그만 남기고 다시 던지지 않는다")
    void deliver_WhenEmailSendFails_ShouldNotThrow() {
        // given
        ReportCreatedEvent event = new ReportCreatedEvent("reported-user", "욕설", "reporter-user", 11L);
        doThrow(new AuthException(ErrorCode.EMAIL_SEND_FAILED))
                .when(emailService).sendReportEmail("reported-user", "욕설", "reporter-user", 11L);

        // when & then
        assertThatCode(() -> reportEmailDeliveryService.deliver(event))
                .doesNotThrowAnyException();
    }
}
