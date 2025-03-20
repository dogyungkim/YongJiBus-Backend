package com.yongjibus.chat.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.chat.domain.ChatMessage;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.controller.dto.ChatMessageDTO;
import com.yongjibus.chat.controller.dto.ChatMessageResponseDTO;
import com.yongjibus.chat.controller.dto.ChatRoomCreateDTO;
import com.yongjibus.chat.controller.dto.ChatRoomResponseDTO;
import com.yongjibus.chat.controller.dto.FcmTokenRegisterRequestDTO;
import com.yongjibus.chat.service.ChatService;
import com.yongjibus.chat.service.FCMTokenService;
import com.yongjibus.global.common.response.ApiResponse;
import com.yongjibus.global.common.response.SliceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final FCMTokenService fcmTokenService;

    // @GetMapping("/rooms")
    // public ResponseEntity<ApiResponse<List<ChatRoomResponseDTO>>> getAllChatRooms() {
    //     List<ChatRoomResponseDTO> chatRooms = chatService.getAllChatRooms()
    //                                             .stream()
    //                                             .map(ChatRoomResponseDTO::from)
    //                                             .collect(Collectors.toList());
    //     return ApiResponse.success(chatRooms);
    // }

    /**
     * 회원이 참여하지 않은 채팅방 목록 조회 엔드포인트
     * 현재 인증된 회원이 참여하지 않은 모든 채팅방 정보를 반환
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDTO>>> getAvailableChatRooms(
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        List<ChatRoomResponseDTO> availableChatRooms = chatService.getNotJoinedChatRooms(memberDetail.getMember())
                .stream()
                .map(ChatRoomResponseDTO::from)
                .collect(Collectors.toList());
        
        return ApiResponse.success(availableChatRooms);
    }
    
    /**
     * 회원이 속한 채팅방 목록 조회 엔드포인트
     * 현재 인증된 회원이 참여 중인 모든 채팅방 정보를 반환
     */
    @GetMapping("/rooms/my")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDTO>>> getMyJoinedChatRooms(
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        List<ChatRoomResponseDTO> myChatRooms = chatService.getMyChatRooms(memberDetail.getMember())
                .stream()
                .map(ChatRoomResponseDTO::from)
                .collect(Collectors.toList());
        
        return ApiResponse.success(myChatRooms);
    }
    
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> createChatRoom(@AuthenticationPrincipal MemberDetail memberDetail, @RequestBody ChatRoomCreateDTO request) {
        ChatRoom chatRoom = chatService.createChatRoom(
            request.getName(), 
            request.getDepartureTime(),
            memberDetail.getMember()
        );
                
        return ApiResponse.success(ChatRoomResponseDTO.from(chatRoom));
    }

    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> joinChatRoom(@AuthenticationPrincipal MemberDetail memberDetail, @PathVariable("roomId") Long roomId) {
        ChatRoom chatRoom = chatService.joinChatRoom(roomId, memberDetail.getMember());
        
        return ApiResponse.success(ChatRoomResponseDTO.from(chatRoom));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<SliceResponse<ChatMessageResponseDTO>>> getChatMessages(
        @PathVariable("roomId") Long roomId,
        @AuthenticationPrincipal MemberDetail memberDetail,
        Pageable pageable
    ) {
        Slice<ChatMessageResponseDTO> messages = chatService.getChatMessages(roomId, memberDetail.getMember(), pageable)
                .map(ChatMessageResponseDTO::from);
                
        return ApiResponse.success(SliceResponse.from(messages));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDTO>> getChatRoom(@PathVariable("roomId") Long roomId) {
        ChatRoom chatRoom = chatService.getChatRoom(roomId);
        return ApiResponse.success(ChatRoomResponseDTO.from(chatRoom));
    }

    @MessageMapping("/chat/message")
    public void sendMessage(@RequestBody ChatMessageDTO message) {
        ChatMessage newMessage = ChatMessage.builder()
                .messageType(message.messageType())
                .content(message.content())
                .sender(message.sender())
                .roomId(message.roomId())
                .createdAt(LocalDateTime.now())
                .build();
        chatService.processAndSendMessage(newMessage);
    }
    
    /**
     * FCM 토큰 등록 엔드포인트
     * 클라이언트에서 FCM 토큰을 서버로 전송하여 사용자와 연결
     */
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<String>> registerFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @RequestBody FcmTokenRegisterRequestDTO requestDTO) {
        
        log.info("FCM 토큰 등록 요청: 사용자 {}", memberDetail.getUsername());
        
        fcmTokenService.saveToken(memberDetail.getMember(), requestDTO.token());
        
        return ApiResponse.success("FCM 토큰 등록 성공");
    }
    
    /**
     * FCM 토큰 삭제 엔드포인트
     * 로그아웃 시 FCM 토큰 제거
     */
    @PostMapping("/fcm-token/remove")
    public ResponseEntity<ApiResponse<String>> removeFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        log.info("FCM 토큰 삭제 요청: 사용자 {}", memberDetail.getUsername());
        FCMToken token = fcmTokenService.getActiveTokenByMember(memberDetail.getMember());

        fcmTokenService.deactivateToken(token);
        
        return ApiResponse.success("FCM 토큰 삭제 성공");
    }

    /**
     * 채팅방 퇴장 엔드포인트
     */
    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<ApiResponse<String>> leaveChatRoom(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @PathVariable("roomId") Long roomId) {
        
        chatService.leaveChatRoom(roomId, memberDetail.getMember());
        
        return ApiResponse.success("채팅방 퇴장 성공");
    }
}
