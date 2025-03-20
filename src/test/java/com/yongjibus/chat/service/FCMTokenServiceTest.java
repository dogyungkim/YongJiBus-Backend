package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.repository.FCMTokenRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FCMTokenServiceTest {

    @Mock
    private FCMTokenRepository fcmTokenRepository;

    @InjectMocks
    private FCMTokenService fcmTokenService;
    
    private Member member;

    @BeforeEach
    void setUp() {
        // Member 엔티티의 실제 빌더 패턴에 맞게 수정
        member = Member.builder()
                .name("테스트")
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();
        
        // 테스트를 위해 id 값 설정 (실제로는 리플렉션을 사용하거나 테스트 헬퍼 메서드를 만들어야 함)
        // 여기서는 간단히 표현만 하고 실제 구현은 생략합니다
    }

    @Test
    @DisplayName("기존 토큰이 있을 경우 토큰을 업데이트한다")
    void saveToken_WithExistingToken_ShouldUpdateToken() {
        // given
        String newToken = "new-fcm-token";
        
        FCMToken existingToken = FCMToken.builder()
                .member(member)
                .token("old-fcm-token")
                .build();
        
        given(fcmTokenRepository.findByMemberAndIsActiveTrue(member))
                .willReturn(Optional.of(existingToken));
        given(fcmTokenRepository.save(any(FCMToken.class)))
                .willReturn(existingToken);

        // when
        fcmTokenService.saveToken(member, newToken);

        // then
        verify(fcmTokenRepository, times(1)).findByMemberAndIsActiveTrue(member);
        verify(fcmTokenRepository, times(1)).save(existingToken);
        assertThat(existingToken.getToken()).isEqualTo(newToken);
    }

    @Test
    @DisplayName("기존 토큰이 없을 경우 새 토큰을 저장한다")
    void saveToken_WithoutExistingToken_ShouldCreateNewToken() {
        // given
        String token = "new-fcm-token";
        
        given(fcmTokenRepository.findByMemberAndIsActiveTrue(member))
                .willReturn(Optional.empty());

        // when
        fcmTokenService.saveToken(member, token);

        // then
        verify(fcmTokenRepository, times(1)).findByMemberAndIsActiveTrue(member);
        verify(fcmTokenRepository, times(1)).save(any(FCMToken.class));
    }

    @Test
    @DisplayName("토큰을 비활성화한다")
    void deactivateToken_ShouldDeactivateToken() {
        // given
        FCMToken token = FCMToken.builder()
                .member(member)
                .token("fcm-token")
                .build();
        
        given(fcmTokenRepository.save(any(FCMToken.class)))
                .willReturn(token);

        // when
        fcmTokenService.deactivateToken(token);

        // then
        verify(fcmTokenRepository, times(1)).save(token);
        assertThat(token.isActive()).isFalse();
    }

    @Test
    @DisplayName("활성화된 토큰을 조회한다")
    void getActiveTokenByMember_WithExistingToken_ShouldReturnToken() {
        // given
        FCMToken token = FCMToken.builder()
                .member(member)
                .token("fcm-token")
                .build();
        
        given(fcmTokenRepository.findByMemberAndIsActiveTrue(member))
                .willReturn(Optional.of(token));

        // when
        FCMToken result = fcmTokenService.getActiveTokenByMember(member);

        // then
        verify(fcmTokenRepository, times(1)).findByMemberAndIsActiveTrue(member);
        assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("활성화된 토큰이 없을 경우 예외를 발생시킨다")
    void getActiveTokenByMember_WithoutExistingToken_ShouldThrowException() {
        // given
        given(fcmTokenRepository.findByMemberAndIsActiveTrue(member))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fcmTokenService.getActiveTokenByMember(member))
                .isInstanceOf(ChatException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FCM_TOKEN_NOT_FOUND);
    }
} 