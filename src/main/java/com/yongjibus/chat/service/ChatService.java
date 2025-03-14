package com.yongjibus.chat.service;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yongjibus.auth.domain.Member;
import com.yongjibus.auth.service.MemberService;
import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.repository.ChatRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.global.exception.ChatException;
import com.yongjibus.global.exception.ErrorCode;
import com.yongjibus.global.websocket.WebsocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    private final MemberService memberService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebsocketSessionManager websocketSessionManager;
    private final FCMNotificationService fcmNotificationService;

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllChatRooms() {
        return chatRoomRepository.findAll();
    }

    @Transactional
    public ChatRoom createChatRoom(String name, LocalTime departureTime, Member member) {
        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .departureTime(departureTime)
                .build();
                
        chatRoomRepository.save(chatRoom);
        setChatRoom(chatRoom, member);
        chatRoomRepository.save(chatRoom);

        return chatRoom;
    }

    @Transactional
    public ChatRoom joinChatRoom(Long roomId, Member member) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 이미 해당 채팅방에 참여 중인지 확인
        if (member.getRoom() != null && member.getRoom().getId().equals(roomId)) {
            log.info("이미 해당 채팅방에 참여 중입니다.");
            return chatRoom;
        }
        
        setChatRoom(chatRoom, member);
        
        // 입장 메시지 생성 및 전송
        ChatMessage enterMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.ENTER)
                .content(member.getUsername() + "님이 입장하셨습니다.")
                .sender(member.getUsername())
                .roomId(roomId)
                .createdAt(LocalDateTime.now())
                .build();
        processAndSendMessage(enterMessage);
        
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void leaveChatRoom(Long roomId, Member member) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        // 해당 채팅방에 참여 중인지 확인
        if (member.getRoom() == null || !member.getRoom().getId().equals(roomId)) {
            log.info("해당 채팅방에 참여 중이 아닙니다.");
            return;
        }
        
        // 채팅방에서 나가기
        member.setRoom(null);
        memberService.saveMember(member);
        
        // 퇴장 메시지 생성 및 전송
        ChatMessage leaveMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.LEAVE)
                .content(member.getUsername() + "님이 퇴장하셨습니다.")
                .sender(member.getUsername())
                .roomId(roomId)
                .createdAt(LocalDateTime.now())
                .build();
        processAndSendMessage(leaveMessage);
    }

    @Transactional(readOnly = true)
    public ChatRoom getChatRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Transactional
    public void processAndSendMessage(ChatMessage message) {
        // 메시지 저장
        chatRepository.save(message);

        // 소켓 관련 로직
        ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId())
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        List<Member> members = chatRoom.getMembers();
        for (Member member : members) {
            if (!websocketSessionManager.isSessionExists(member.getEmail())) {
                // 일반 채팅 메시지인 경우에만 알림 전송 (입장/퇴장 메시지는 알림 제외)
                if (message.getMessageType() == ChatMessage.MessageType.MESSAGE) {
                    fcmNotificationService.sendChatNotification(message, member, chatRoom);
                }
            }
        }
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }

    @Transactional(readOnly = true)
    public Slice<ChatMessage> getChatMessages(Long roomId, Pageable pageable) {
        // 최신순으로 데이터를 가져옴
        Slice<ChatMessage> messages = chatRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        
        // 결과를 리스트로 변환하고 순서를 뒤집음
        List<ChatMessage> reversedContent = new ArrayList<>(messages.getContent());
        Collections.reverse(reversedContent);
        
        // 뒤집은 리스트로 새로운 SliceImpl 생성
        return new SliceImpl<>(
            reversedContent, 
            messages.getPageable(), 
            messages.hasNext()
        );
    }


    private void setChatRoom(ChatRoom chatRoom, Member member) {
        member.setRoom(chatRoom);
        memberService.saveMember(member);
    }
}