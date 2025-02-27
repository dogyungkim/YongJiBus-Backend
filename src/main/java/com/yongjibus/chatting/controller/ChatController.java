package com.yongjibus.chatting.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yongjibus.chatting.domain.ChatRoom;
import com.yongjibus.chatting.domain.dto.ChatMessageDTO;
import com.yongjibus.chatting.domain.dto.ChatRoomCreateDTO;
import com.yongjibus.chatting.domain.dto.ChatRoomResponseDTO;
import com.yongjibus.chatting.service.ChatRoomService;
import com.yongjibus.global.ApiResponse;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatRoomService chatRoomService;

    @MessageMapping("/message")
    public void sendMessage(ChatMessageDTO message) {
        template.convertAndSend("/sub/chat/room", message);
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDTO>>> getAllChatRooms() {
        List<ChatRoomResponseDTO> chatRooms = chatRoomService.getAllChatRooms()
                                                .stream()
                                                .map(ChatRoomResponseDTO::from)
                                                .collect(Collectors.toList());
        return ApiResponse.success(chatRooms);
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> createChatRoom(@RequestBody ChatRoomCreateDTO request) {
        ChatRoom chatRoom = chatRoomService.createChatRoom(
            request.getName(), 
            request.getDepartureTime()
        );
        
        ChatRoomResponseDTO response = ChatRoomResponseDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .departureTime(chatRoom.getDepartureTime())
                .createdAt(chatRoom.getCreatedAt())
                .build();
                
        return ApiResponse.success(response);
    }
}
