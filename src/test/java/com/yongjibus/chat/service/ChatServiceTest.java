package com.yongjibus.chat.service;

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.ChatRoomMember;
import com.yongjibus.chat.repository.ChatRepository;
import com.yongjibus.chat.repository.ChatRoomMemberRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.global.infra.websocket.WebsocketSessionManager;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.service.MemberService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatRepository chatRepository;
    
    @Mock
    private MemberService memberService;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private WebsocketSessionManager websocketSessionManager;
    
    @Mock
    private FCMNotificationService fcmNotificationService;
    
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    
    @InjectMocks
    private ChatService chatService;
    
    private Member testMember;
    private Member testMember2;
    private ChatRoom testChatRoom;
    private ChatMessage testChatMessage;
    private ChatRoomMember testChatRoomMember;
    
    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .email("test@example.com")
                .name("이영진")
                .username("테스트유저")
                .build();
        
        testMember2 = Member.builder()
                .id(2L)
                .email("test2@example.com")
                .name("이영진2")
                .username("테스트유저2")
                .build();
                
        testChatRoom = ChatRoom.builder()
                .name("테스트 채팅방")
                .departureTime(LocalTime.of(14, 0))
                .build();
                
        testChatMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.MESSAGE)
                .content("테스트 메시지")
                .sender("테스트유저")
                .roomId(1L)
                .createdAt(LocalDateTime.now())
                .build();
                
        testChatRoomMember = ChatRoomMember.builder()
                .member(testMember)
                .chatRoom(testChatRoom)
                .joinedAt(LocalDateTime.now())
                .active(true)
                .build();
    }
    
    @Test
    @DisplayName("채팅방 생성 테스트")
    void createChatRoomTest() {
        // given
        String roomName = "새로운 채팅방";
        LocalTime departureTime = LocalTime.of(15, 30);
        
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
        
        // when
        ChatRoom result = chatService.createChatRoom(roomName, departureTime, testMember);
        
        // then
        assertThat(result).isNotNull();
        verify(chatRoomRepository, times(2)).save(any(ChatRoom.class));
    }
    
    @Test
    @DisplayName("채팅방 참여 테스트")
    void joinChatRoomTest() {
        // given
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(ChatMessage.class));
        
        // when
        ChatRoom result = chatService.joinChatRoom(1L, testMember);
        
        // then
        assertThat(result).isNotNull();
        verify(chatRepository, times(1)).save(any(ChatMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("채팅방 조회 테스트")
    void getChatRoomTest() {
        // given
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));
        
        // when
        ChatRoom result = chatService.getChatRoom(1L);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 채팅방");
    }
    
    @Test
    @DisplayName("채팅방 조회 실패 테스트")
    void getChatRoomNotFoundTest() {
        // given
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // when & then
        assertThrows(ChatException.class, () -> chatService.getChatRoom(1L));
    }

    // TODO: 멤버가 속한 채팅방 불러오기 테스트 추가
    @Test
    @DisplayName("멤버가 속한 채팅방 불러오기")
    void getChatRoomByMemberTest() {
        // // given
        // when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));
        // when(chatRoomMemberRepository.findByMemberAndChatRoomAndActiveTrue(any(), any()))
        //     .thenReturn(Optional.of(testChatRoomMember));

        // // when
        // ChatRoom result = chatService.getChatRoomByMember(testMember);
        
        
    }
    
    @Test
    @DisplayName("메시지 처리 및 전송 테스트")
    void processAndSendMessageTest() {
        // given
        testChatRoom.addMember(testMember);
        
        when(chatRepository.save(any(ChatMessage.class))).thenReturn(testChatMessage);
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));
        when(websocketSessionManager.isSessionExists(anyString())).thenReturn(false);
        doNothing().when(fcmNotificationService).sendChatNotification(any(), any(), any());
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(ChatMessage.class));
        
        // when
        chatService.processAndSendMessage(testChatMessage);
        
        // then
        verify(chatRepository, times(1)).save(any(ChatMessage.class));
        verify(fcmNotificationService, times(1)).sendChatNotification(any(), any(), any());
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("채팅 메시지 조회 테스트")
    void getChatMessagesTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ChatMessage> messages = Arrays.asList(testChatMessage);
        Slice<ChatMessage> slice = new SliceImpl<>(messages, pageable, false);
        
        when(chatRoomRepository.findById(anyLong())).thenReturn(Optional.of(testChatRoom));
        when(chatRoomMemberRepository.findJoinedAtByMemberAndChatRoom(any(), any()))
            .thenReturn(Optional.of(LocalDateTime.now().minusHours(1)));
        when(chatRepository.findMessagesAfterJoinTime(
            anyLong(), any(LocalDateTime.class), any(Pageable.class))).thenReturn(slice);
        
        // when
        Slice<ChatMessage> result = chatService.getChatMessages(1L, testMember, pageable);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 메시지");
    }
}