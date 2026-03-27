package com.yongjibus.global.infra.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.member.domain.MemberReport;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.report.to}")
    private String reportTo;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Thymeleaf 템플릿 엔진 주입

    public void sendAuthEmail(String to, String authCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("인증 코드");
            
            Context context = new Context();
            context.setVariable("authCode", authCode);
            
            String htmlContent = templateEngine.process("auth-email-template", context);
            helper.setText(htmlContent, true); // true는 HTML 내용임을 나타냄
            
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new AuthException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    public void sendReportEmail(MemberReport userReport) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(reportTo);
            helper.setSubject("신고 처리");
            
            Context context = new Context();
            context.setVariable("reportedUser", userReport.getReportedMember().getUsername());
            context.setVariable("reason", userReport.getReason());
            context.setVariable("reporter", userReport.getReporter().getUsername());
            context.setVariable("roomId", userReport.getRoomId());
            
            String htmlContent = templateEngine.process("report-email-template", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new AuthException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
} 
