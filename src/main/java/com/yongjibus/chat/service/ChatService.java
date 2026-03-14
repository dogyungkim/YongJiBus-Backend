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

import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.repository.ChatRepository;
import com.yongjibus.chat.repository.ChatRoomRepository;
import com.yongjibus.chat.repository.ChatRoomMemberRepository;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.global.infra.websocket.WebsocketSessionManager;
import com.yongjibus.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final WebsocketSessionManager websocketSessionManager;
    private final FCMNotificationService fcmNotificationService;

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllChatRooms() {
        return chatRoomRepository.findAll().stream().filter(chatRoom -> chatRoom.getMembers().size() > 0).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getMyChatRooms(Member member) {
        return chatRoomMemberRepository.findActiveChatRoomsByMember(member);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getNotJoinedChatRooms(Member member) {
        return chatRoomMemberRepository.findChatRoomsNotJoinedByMember(member).stream().filter(chatRoom -> chatRoom.getMembers().size() > 0).toList();
    }

    @Transactional
    public ChatRoom createChatRoom(String name, LocalTime departureTime, Member member) {
        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .departureTime(departureTime)
                .build();
                
        chatRoomRepository.save(chatRoom);
        chatRoom.addMember(member);
        
        // 채팅방 생성 후 욕설 관련 경고 메시지 추가
        ChatMessage warningMessage = ChatMessage.builder()
                .messageType(ChatMessage.MessageType.SYSTEM)
                .content("이 채팅방에서는 욕설, 비방, 차별적 발언 등이 금지됩니다. 위반 시 서비스 이용이 제한될 수 있습니다.")
                .sender("시스템")
                .roomId(chatRoom.getId())
                .createdAt(LocalDateTime.now())
                .build();
        
        // 메시지 저장 및 전송
        chatRepository.save(warningMessage);
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatRoom.getId(), warningMessage);
        
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public ChatRoom joinChatRoom(Long roomId, Member member) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        if (chatRoom.getMembers().size() >= 5){
            throw new ChatException(ErrorCode.CHAT_ROOM_FULL);
        }

        // 이미 해당 채팅방에 참여 중인지 확인
        boolean alreadyJoined = chatRoomMemberRepository
            .findByMemberAndChatRoomAndActiveTrue(member, chatRoom)
            .isPresent();
        
        if (alreadyJoined) {
            return chatRoom;
        }
        
        // 채팅방에 멤버 추가
        chatRoom.addMember(member);
        
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

        boolean isMemberInChatRoom = chatRoom.getMembers().stream()
            .anyMatch(m -> m.getId().equals(member.getId()));

        if (!isMemberInChatRoom) {
            throw new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        
        // 채팅방에서 멤버 제거
        chatRoom.removeMember(member);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 빈 채팅방 제거
        if (savedChatRoom.getMembers().size() == 0) {
            chatRoomRepository.delete(savedChatRoom);
            return ;
        }
        
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
        try{
            chatRepository.save(message);
        } catch (Exception e) {
            log.error("메시지 저장 중 오류 발생: {}", e.getMessage());
            throw new ChatException(ErrorCode.CHAT_MESSAGE_SAVE_ERROR);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId())
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        List<Member> members = chatRoom.getMembers();
        for (Member member : members) {
            if (!websocketSessionManager.isSessionExists(member.getEmail())) {
                // 일반 채팅 메시지인 경우에만 알림 전송 (입장/퇴장 메시지는 알림 제외)
                if (message.getMessageType() == ChatMessage.MessageType.MESSAGE) {
                    try {
                        fcmNotificationService.sendChatNotification(message, member, chatRoom);
                    } catch (Exception e) {
                        log.warn("FCM 알림 전송 실패로 푸시를 건너뜁니다. roomId={}, memberId={}", message.getRoomId(), member.getId(), e);
                    }
                }
            }
        }
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }

    @Transactional(readOnly = true)
    public Slice<ChatMessage> getChatMessages(Long roomId, Member member, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        // 사용자의 채팅방 입장 시간 조회
        LocalDateTime joinedAt = chatRoomMemberRepository
            .findJoinedAtByMemberAndChatRoom(member, chatRoom)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 입장 시간 이후의 메시지만 조회
        Slice<ChatMessage> messages = chatRepository.findMessagesAfterJoinTime(roomId, joinedAt, pageable);
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
}
