package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.repository.FCMTokenRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FCMTokenService {

    private final FCMTokenRepository fcmTokenRepository;

    @Transactional
    public void registerAnonymousToken(String token) {
        List<FCMToken> existingTokens = fcmTokenRepository.findAllByTokenForUpdate(token);
        if (existingTokens.stream().anyMatch(FCMToken::isActive)) {
            return;
        }
        for (FCMToken fcmToken : existingTokens) {
            if (fcmToken.getMember() == null) {
                fcmToken.reactivate();
                fcmTokenRepository.save(fcmToken);
                return;
            }
        }
        fcmTokenRepository.save(new FCMToken(token, null));
    }

    @Transactional
    public void bindToken(Member member, String token) {
        List<FCMToken> existingTokens = fcmTokenRepository.findAllByTokenForUpdate(token);
        for (FCMToken fcmToken : existingTokens) {
            if (fcmToken.isActive() && fcmToken.getMember() != null && !ownedBy(fcmToken, member)) {
                throw new ChatException(ErrorCode.FCM_TOKEN_CONFLICT);
            }
        }
        if (existingTokens.stream().anyMatch(fcmToken -> fcmToken.isActive() && ownedBy(fcmToken, member))) {
            return;
        }
        for (FCMToken fcmToken : existingTokens) {
            if (fcmToken.isActive() || fcmToken.getMember() == null || ownedBy(fcmToken, member)) {
                fcmToken.bind(member);
                fcmToken.reactivate();
                fcmTokenRepository.save(fcmToken);
                return;
            }
        }

        fcmTokenRepository.save(new FCMToken(token, member));
    }

    @Transactional(readOnly = true)
    public List<FCMToken> findActiveTokensByMember(Member member) {
        return fcmTokenRepository.findAllByMemberAndIsActiveTrue(member);
    }

    @Transactional
    public void unbindToken(Member member, String token) {
        fcmTokenRepository.findAllByTokenForUpdate(token).stream()
                .filter(fcmToken -> ownedBy(fcmToken, member))
                .forEach(FCMToken::unbind);
    }

    @Transactional
    public void deactivateToken(Member member, String token) {
        fcmTokenRepository.findAllByTokenForUpdate(token).stream()
                .filter(fcmToken -> member == null ? fcmToken.getMember() == null : ownedBy(fcmToken, member))
                .forEach(FCMToken::deactivate);
    }

    @Transactional
    public void unbindAllTokens(Member member) {
        fcmTokenRepository.findAllByMember(member).forEach(FCMToken::unbind);
    }

    private boolean ownedBy(FCMToken token, Member member) {
        return token.getMember() != null && member.getId().equals(token.getMember().getId());
    }
}
