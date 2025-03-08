package com.yongjibus.chat.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.dto.ChatMessageDTO;
import com.yongjibus.chat.domain.dto.ChatMessageResponseDTO;
import com.yongjibus.chat.domain.dto.ChatRoomCreateDTO;
import com.yongjibus.chat.domain.dto.ChatRoomResponseDTO;
import com.yongjibus.chat.service.ChatService;
import com.yongjibus.global.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDTO>>> getAllChatRooms() {
        List<ChatRoomResponseDTO> chatRooms = chatService.getAllChatRooms()
                                                .stream()
                                                .map(ChatRoomResponseDTO::from)
                                                .collect(Collectors.toList());
        return ApiResponse.success(chatRooms);
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> createChatRoom(@AuthenticationPrincipal MemberDetail memberDetail, @RequestBody ChatRoomCreateDTO request) {
        ChatRoom chatRoom = chatService.createChatRoom(
            request.getName(), 
            request.getDepartureTime(),
            memberDetail.getMember()
        );
        
        ChatRoomResponseDTO response = ChatRoomResponseDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .departureTime(chatRoom.getDepartureTime())
                .createdAt(chatRoom.getCreatedAt())
                .build();
                
        return ApiResponse.success(response);
    }

    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> joinChatRoom(@AuthenticationPrincipal MemberDetail memberDetail, @PathVariable("roomId") Long roomId) {
        ChatRoom chatRoom = chatService.joinChatRoom(roomId, memberDetail.getMember());
        
        ChatRoomResponseDTO response = ChatRoomResponseDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .departureTime(chatRoom.getDepartureTime())
                .createdAt(chatRoom.getCreatedAt())
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponseDTO>>> getChatMessages(@PathVariable("roomId") Long roomId) {
        List<ChatMessageResponseDTO> messages = chatService.getChatMessages(roomId)
                .stream()
                .map(ChatMessageResponseDTO::from)
                .collect(Collectors.toList());
        return ApiResponse.success(messages);
    }

    @MessageMapping("/chat/message")
    public void sendMessage(@RequestBody ChatMessageDTO message) {
        ChatMessage newMessage = ChatMessage.builder()
                .content(message.content())
                .sender(message.sender())
                .roomId(message.roomId())
                .createdAt(message.createdAt())
                .build();
        
        // 서비스 계층으로 메시지 처리 로직 위임
        // 1. 메시지 저장
        // 2. 사용자 세션 확인
        // 3. 온라인 사용자에게는 WebSocket으로 메시지 전송
        // 4. 오프라인 사용자에게는 FCM으로 알림 전송
        chatService.processAndSendMessage(newMessage);
    }
}
