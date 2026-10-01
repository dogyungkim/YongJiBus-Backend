package com.yongjibus.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.repository.FCMTokenRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;

@ExtendWith(MockitoExtension.class)
class FCMTokenServiceTest {

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @InjectMocks
    private FCMTokenService fcmTokenService;

    @Test
    void publicRegistrationDoesNotChangeMemberOwnedToken() {
        Member owner = member(1L);
        FCMToken token = FCMToken.builder().member(owner).token("fcm-token").build();
        given(fcmTokenRepository.findAllByTokenForUpdate("fcm-token")).willReturn(List.of(token));

        fcmTokenService.registerAnonymousToken("fcm-token");

        assertThat(token.getMember()).isEqualTo(owner);
        verify(fcmTokenRepository, never()).save(any());
    }

    @Test
    void publicRegistrationDoesNotReactivateDuplicateOfActiveMemberToken() {
        FCMToken memberToken = FCMToken.builder().member(member(1L)).token("fcm-token").build();
        FCMToken duplicate = FCMToken.builder().token("fcm-token").build();
        duplicate.deactivate();
        given(fcmTokenRepository.findAllByTokenForUpdate("fcm-token"))
                .willReturn(List.of(duplicate, memberToken));

        fcmTokenService.registerAnonymousToken("fcm-token");

        assertThat(duplicate.isActive()).isFalse();
        verify(fcmTokenRepository, never()).save(any());
    }

    @Test
    void bindRejectsTokenOwnedByAnotherMember() {
        given(fcmTokenRepository.findAllByTokenForUpdate("fcm-token"))
                .willReturn(List.of(FCMToken.builder().member(member(1L)).token("fcm-token").build()));

        assertThatThrownBy(() -> fcmTokenService.bindToken(member(2L), "fcm-token"))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FCM_TOKEN_CONFLICT);
    }

    @Test
    void anonymousTokenCanBindUnbindAndRegisterAgain() {
        Member owner = member(1L);
        FCMToken token = FCMToken.builder().token("fcm-token").build();
        token.deactivate();
        given(fcmTokenRepository.findAllByTokenForUpdate("fcm-token"))
                .willReturn(List.of(token), List.of(token), List.of(token), List.of(token));

        fcmTokenService.registerAnonymousToken("fcm-token");
        assertThat(token.isActive()).isTrue();
        fcmTokenService.bindToken(owner, "fcm-token");
        assertThat(token.getMember()).isEqualTo(owner);
        fcmTokenService.unbindToken(owner, "fcm-token");
        assertThat(token.getMember()).isNull();
        fcmTokenService.registerAnonymousToken("fcm-token");

        assertThat(token.isActive()).isTrue();
        assertThat(token.getMember()).isNull();
        verify(fcmTokenRepository, times(2)).save(token);
    }

    @Test
    void unbindOnlyClearsTheCurrentMemberAndDeletionClearsEveryDevice() {
        Member owner = member(1L);
        Member other = member(2L);
        FCMToken first = FCMToken.builder().member(owner).token("first").build();
        FCMToken second = FCMToken.builder().member(owner).token("second").build();
        given(fcmTokenRepository.findAllByTokenForUpdate("first")).willReturn(List.of(first), List.of(first));
        given(fcmTokenRepository.findAllByMember(owner)).willReturn(List.of(first, second));

        fcmTokenService.unbindToken(other, "first");
        assertThat(first.getMember()).isEqualTo(owner);

        fcmTokenService.unbindToken(owner, "first");
        assertThat(first.getMember()).isNull();

        fcmTokenService.unbindAllTokens(owner);
        assertThat(second.getMember()).isNull();
    }

    @Test
    void unbindMatchesMemberIdAcrossDifferentEntityInstances() {
        FCMToken token = FCMToken.builder().member(member(1L)).token("fcm-token").build();
        given(fcmTokenRepository.findAllByTokenForUpdate("fcm-token")).willReturn(List.of(token));

        fcmTokenService.unbindToken(member(1L), "fcm-token");

        assertThat(token.getMember()).isNull();
    }

    @Test
    void deactivationOnlyAffectsTheTokenOwnerAndBindingReactivatesIt() {
        Member owner = member(1L);
        FCMToken token = FCMToken.builder().member(owner).token("fcm-token").build();
        given(fcmTokenRepository.findAllByTokenForUpdate("fcm-token")).willReturn(List.of(token));

        fcmTokenService.deactivateToken(null, "fcm-token");
        fcmTokenService.deactivateToken(member(2L), "fcm-token");
        assertThat(token.isActive()).isTrue();

        fcmTokenService.deactivateToken(owner, "fcm-token");
        assertThat(token.isActive()).isFalse();

        fcmTokenService.bindToken(owner, "fcm-token");
        assertThat(token.isActive()).isTrue();
        assertThat(token.getMember()).isEqualTo(owner);
    }

    @Test
    void guestCanDeactivateAndReactivateItsOwnToken() {
        FCMToken token = FCMToken.builder().token("guest-token").build();
        given(fcmTokenRepository.findAllByTokenForUpdate("guest-token")).willReturn(List.of(token));

        fcmTokenService.deactivateToken(null, "guest-token");
        assertThat(token.isActive()).isFalse();
        fcmTokenService.registerAnonymousToken("guest-token");
        assertThat(token.isActive()).isTrue();
    }

    private static Member member(Long id) {
        Member member = Member.builder().username("member-" + id).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
