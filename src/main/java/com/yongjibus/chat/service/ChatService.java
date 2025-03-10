package com.yongjibus.chat.service;

import java.time.LocalTime;
import java.util.List;

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

        return chatRoom;
    }

    @Transactional
    public ChatRoom joinChatRoom(Long roomId, Member member) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 이미 해당 채팅방에 참여 중인지 확인
        if (member.getRoom() != null && member.getRoom().getId().equals(roomId)) {
            return chatRoom;
        }
        
        member.setRoom(chatRoom);
        
        return chatRoomRepository.save(chatRoom);
    }

    @Transactional
    public void processAndSendMessage(ChatMessage message) {
        //메시지 저장
        chatRepository.save(message);

        //소켓 관련 로직
        ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId())
            .orElseThrow(() -> new ChatException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        
        List<Member> members = chatRoom.getMembers();
        for (Member member : members) {
            if (!websocketSessionManager.isSessionExists(member.getEmail())) {
                log.info("Member {} has no session", member.getEmail());
            }
        }
        // 사용자와 세션이 유지되고 있으면 메시지 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
        // 사용자와 세션이 해제 된 경우 알림 전송   
        // sendNotificationToOfflineUsers(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatMessages(Long roomId) {
        return chatRepository.findByRoomId(roomId);
    }


    private void setChatRoom(ChatRoom chatRoom, Member member) {
        member.setRoom(chatRoom);
        memberService.saveMember(member);
    }
}