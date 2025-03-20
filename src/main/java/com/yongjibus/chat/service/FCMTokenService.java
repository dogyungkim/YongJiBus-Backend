package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.repository.FCMTokenRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FCMTokenService {

    private final FCMTokenRepository fcmTokenRepository;

    @Transactional
    public void saveToken(Member member, String token) {
        Optional<FCMToken> existingToken = fcmTokenRepository.findByMemberAndIsActiveTrue(member);
        
        if (existingToken.isPresent()) {
            FCMToken fcmToken = existingToken.get();
            fcmToken.updateToken(token);
            fcmTokenRepository.save(fcmToken);
        } else {
            FCMToken newToken = FCMToken.builder()
                    .member(member)
                    .token(token)
                    .build();
            fcmTokenRepository.save(newToken);
        }
    }

    @Transactional
    public void deactivateToken(FCMToken token) {
        token.deactivate();
        fcmTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public FCMToken getActiveTokenByMember(Member member) {
        Optional<FCMToken> fcmToken = fcmTokenRepository.findByMemberAndIsActiveTrue(member);
        if(!fcmToken.isPresent()){
            throw new ChatException(ErrorCode.FCM_TOKEN_NOT_FOUND);
        }

        return fcmToken.get();
    }
} 