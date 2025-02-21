package com.yongjibus.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAuthEmail(String to, String authCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("인증 코드");
        message.setText("인증 코드는 " + authCode + " 입니다.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new AuthException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
} 