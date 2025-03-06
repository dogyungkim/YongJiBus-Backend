package com.yongjibus.chat.service;

import com.yongjibus.auth.domain.Member;
import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.repository.FCMTokenRepository;
import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;

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
    public void deactivateToken(String token) {
        Optional<FCMToken> fcmToken = fcmTokenRepository.findByTokenAndIsActiveTrue(token);
        fcmToken.ifPresent(t -> {
            t.deactivate();
            fcmTokenRepository.save(t);
        });
    }

    @Transactional(readOnly = true)
    public FCMToken getActiveTokenByMember(Member member) {
        Optional<FCMToken> fcmToken = fcmTokenRepository.findByMemberAndIsActiveTrue(member);
        if(!fcmToken.isPresent()){
            //TODO: 커스텀 Exception 만들기
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        return fcmToken.get();
    }
} 