package com.yongjibus.global.infra.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.test.util.ReflectionTestUtils;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("메일 전송기가 MailException을 던지면 EMAIL_SEND_FAILED로 변환한다")
    void sendAuthEmail_WhenMailSenderThrowsMailException_ShouldThrowAuthException() {
        // given
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<p>code</p>");
        doThrow(new MailSendException("mail send failed")).when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailService.sendAuthEmail("test@example.com", "123456"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_SEND_FAILED);
    }

    @Test
    @DisplayName("신고 메일 전송기도 MailException을 EMAIL_SEND_FAILED로 변환한다")
    void sendReportEmail_WhenMailSenderThrowsMailException_ShouldThrowAuthException() {
        // given
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        ReflectionTestUtils.setField(emailService, "reportTo", "ops@example.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<p>report</p>");
        doThrow(new MailSendException("mail send failed")).when(mailSender).send(any(MimeMessage.class));

        // when & then
        assertThatThrownBy(() -> emailService.sendReportEmail("reported", "욕설", "reporter", 12L))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_SEND_FAILED);
    }
}
