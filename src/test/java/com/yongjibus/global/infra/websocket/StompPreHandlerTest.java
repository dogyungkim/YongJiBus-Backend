package com.yongjibus.global.infra.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.auth.service.MemberDetailService;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.ChatRoomMember;
import com.yongjibus.chat.repository.ChatRoomMemberRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.StompException;
import com.yongjibus.global.infra.jwt.JwtService;
import com.yongjibus.member.domain.Member;

@ExtendWith(MockitoExtension.class)
class StompPreHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private MemberDetailService memberDetailService;

    @Mock
    private WebsocketSessionManager websocketSessionManager;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private StompPreHandler stompPreHandler;

    private Member member;
    private MemberDetail memberDetail;
    private Authentication authentication;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트")
                .username("test-user")
                .build();
        memberDetail = new MemberDetail(member);
        authentication = new UsernamePasswordAuthenticationToken(memberDetail, null, memberDetail.getAuthorities());
        chatRoom = ChatRoom.builder()
                .name("테스트 채팅방")
                .departureTime(LocalTime.NOON)
                .build();
    }

    @Test
    @DisplayName("CONNECT 시 access token으로 STOMP 세션을 인증한다")
    void preSend_WhenConnect_ShouldAuthenticateSession() {
        // given
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, accessor -> {
            accessor.setNativeHeader("Authorization", "Bearer valid-token");
            accessor.setSessionId("session-1");
        });
        when(jwtService.validateAccessToken("valid-token")).thenReturn(true);
        when(jwtService.getEmailFromToken("valid-token")).thenReturn(member.getEmail());
        when(memberDetailService.loadUserByUsername(member.getEmail())).thenReturn(memberDetail);

        // when
        Message<?> result = stompPreHandler.preSend(message, messageChannel);

        // then
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isInstanceOf(Authentication.class);
        verify(websocketSessionManager).addSession("session-1", member.getEmail());
    }

    @Test
    @DisplayName("CONNECT 시 Authorization 헤더가 없으면 거절한다")
    void preSend_WhenConnectWithoutAuthorization_ShouldThrowUnauthorized() {
        // given
        Message<byte[]> message = buildMessage(StompCommand.CONNECT, accessor -> accessor.setSessionId("session-1"));

        // when & then
        assertThatThrownBy(() -> stompPreHandler.preSend(message, messageChannel))
                .isInstanceOf(StompException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("채팅방 참여자는 해당 방을 구독할 수 있다")
    void preSend_WhenSubscribeByRoomMember_ShouldAllow() {
        // given
        Message<byte[]> message = buildMessage(StompCommand.SUBSCRIBE, accessor -> {
            accessor.setDestination("/sub/chat/room/1");
            accessor.setUser(authentication);
        });
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(member, chatRoom))
                .thenReturn(Optional.of(ChatRoomMember.builder()
                        .member(member)
                        .chatRoom(chatRoom)
                        .active(true)
                        .build()));

        // when & then
        assertThatCode(() -> stompPreHandler.preSend(message, messageChannel))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 해당 방을 구독할 수 없다")
    void preSend_WhenSubscribeByNonMember_ShouldThrowForbidden() {
        // given
        Message<byte[]> message = buildMessage(StompCommand.SUBSCRIBE, accessor -> {
            accessor.setDestination("/sub/chat/room/1");
            accessor.setUser(authentication);
        });
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(member, chatRoom))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> stompPreHandler.preSend(message, messageChannel))
                .isInstanceOf(StompException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHAT_ROOM_FORBIDDEN);
    }

    private Message<byte[]> buildMessage(StompCommand command, java.util.function.Consumer<StompHeaderAccessor> customizer) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        customizer.accept(accessor);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
