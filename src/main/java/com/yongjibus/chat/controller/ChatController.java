package com.yongjibus.chat.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.controller.dto.ChatMessageDTO;
import com.yongjibus.chat.controller.dto.ChatMessageResponseDTO;
import com.yongjibus.chat.controller.dto.ChatRoomCreateDTO;
import com.yongjibus.chat.controller.dto.ChatRoomResponseDTO;
import com.yongjibus.chat.controller.dto.FcmTokenRegisterRequestDTO;
import com.yongjibus.chat.service.ChatService;
import com.yongjibus.chat.service.FCMTokenService;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.global.common.response.SliceResponse;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.StompException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
@Tag(name = "채팅 API", description = "채팅방 관리, 메시지 교환 및 알림 관련 API")
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
    @Operation(summary = "참여 가능한 채팅방 목록 조회", description = "현재 인증된 회원이 참여하지 않은 모든 채팅방 목록을 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/rooms")
    public ResponseEntity<YongJiResponse<List<ChatRoomResponseDTO>>> getAvailableChatRooms(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        List<ChatRoomResponseDTO> availableChatRooms = chatService.getNotJoinedChatRooms(memberDetail.getMember())
                .stream()
                .map(ChatRoomResponseDTO::from)
                .collect(Collectors.toList());
        
        return YongJiResponse.success(availableChatRooms);
    }
    
    /**
     * 회원이 속한 채팅방 목록 조회 엔드포인트
     * 현재 인증된 회원이 참여 중인 모든 채팅방 정보를 반환
     */
    @Operation(summary = "내가 참여 중인 채팅방 목록 조회", description = "로그인한 사용자가 현재 참여 중인 모든 채팅방 목록을 반환합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "내 채팅방 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @GetMapping("/rooms/my")
    public ResponseEntity<YongJiResponse<List<ChatRoomResponseDTO>>> getMyJoinedChatRooms(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        List<ChatRoomResponseDTO> myChatRooms = chatService.getMyChatRooms(memberDetail.getMember())
                .stream()
                .map(ChatRoomResponseDTO::from)
                .collect(Collectors.toList());
        
        return YongJiResponse.success(myChatRooms);
    }
    
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성하고 생성자는 자동으로 해당 채팅방에 참여합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PostMapping("/rooms")
    public ResponseEntity<YongJiResponse<ChatRoomResponseDTO>> createChatRoom(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail, 
            @Parameter(description = "채팅방 생성 정보", required = true)
            @Valid @RequestBody ChatRoomCreateDTO request) {
        ChatRoom chatRoom = chatService.createChatRoom(
            request.getName(), 
            request.getDepartureTime(),
            memberDetail.getMember()
        );
                
        return YongJiResponse.success(ChatRoomResponseDTO.from(chatRoom));
    }

    @Operation(summary = "채팅방 참여", description = "지정된 ID의 채팅방에 참여합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 참여 성공"),
        @ApiResponse(responseCode = "400", description = "채팅방 인원 초과", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @PostMapping("/rooms/{roomId}")
    public ResponseEntity<YongJiResponse<ChatRoomResponseDTO>> joinChatRoom(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail, 
            @Parameter(description = "참여할 채팅방 ID", required = true)
            @PathVariable("roomId") Long roomId) {
        ChatRoom chatRoom = chatService.joinChatRoom(roomId, memberDetail.getMember());
        
        return YongJiResponse.success(ChatRoomResponseDTO.from(chatRoom));
    }

    @Operation(summary = "채팅 메시지 조회", description = "특정 채팅방의 메시지 기록을 페이지네이션하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
        @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아닌 경우", content = @Content),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<YongJiResponse<SliceResponse<ChatMessageResponseDTO>>> getChatMessages(
        @Parameter(description = "채팅방 ID", required = true)
        @PathVariable("roomId") Long roomId,
        @Parameter(description = "현재 인증된 사용자", hidden = true)
        @AuthenticationPrincipal MemberDetail memberDetail,
        @Parameter(description = "페이지네이션 정보")
        Pageable pageable
    ) {
        Slice<ChatMessageResponseDTO> messages = chatService.getChatMessages(roomId, memberDetail.getMember(), pageable)
                .map(ChatMessageResponseDTO::from);
                
        return YongJiResponse.success(SliceResponse.from(messages));
    }

    @Operation(summary = "채팅방 정보 조회", description = "특정 채팅방의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<YongJiResponse<ChatRoomResponseDTO>> getChatRoom(
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable("roomId") Long roomId) {
        ChatRoom chatRoom = chatService.getChatRoom(roomId);
        return YongJiResponse.success(ChatRoomResponseDTO.from(chatRoom));
    }

    @Operation(summary = "메시지 전송", description = "WebSocket을 통해 채팅 메시지를 전송합니다.", hidden = true)
    @MessageMapping("/chat/message")
    public void sendMessage(
            @Parameter(description = "전송할 메시지 정보", required = true)
            @Valid @Payload ChatMessageDTO message,
            Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof MemberDetail memberDetail)) {
            throw new StompException(ErrorCode.UNAUTHORIZED);
        }

        chatService.sendMessage(memberDetail.getMember(), message.roomId(), message.content());
    }
    
    /**
     * FCM 토큰 등록 엔드포인트
     * 클라이언트에서 FCM 토큰을 서버로 전송하여 사용자와 연결
     */
    @Operation(summary = "FCM 토큰 등록", description = "알림 수신을 위한 Firebase Cloud Messaging 토큰을 등록합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PostMapping("/fcm-token")
    public ResponseEntity<YongJiResponse<String>> registerFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @Parameter(description = "FCM 토큰 정보", required = true)
            @Valid @RequestBody FcmTokenRegisterRequestDTO requestDTO) {
        if (memberDetail == null) {
            fcmTokenService.registerAnonymousToken(requestDTO.token());
        } else {
            fcmTokenService.bindToken(memberDetail.getMember(), requestDTO.token());
        }
        return YongJiResponse.success("FCM 토큰 등록 성공");
    }

    @Operation(summary = "로그인 기기 연결", description = "FCM 토큰을 현재 로그인한 회원에게 연결합니다.")
    @PostMapping("/fcm-token/bind")
    public ResponseEntity<YongJiResponse<String>> bindFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @Valid @RequestBody FcmTokenRegisterRequestDTO requestDTO) {
        fcmTokenService.bindToken(memberDetail.getMember(), requestDTO.token());
        return YongJiResponse.success("FCM 토큰 연결 성공");
    }

    @Operation(summary = "현재 기기 FCM 토큰 비활성화", description = "익명 토큰 또는 현재 회원의 토큰을 비활성화합니다.")
    @PostMapping("/fcm-token/deactivate")
    public ResponseEntity<YongJiResponse<String>> deactivateFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @Valid @RequestBody FcmTokenRegisterRequestDTO requestDTO) {
        fcmTokenService.deactivateToken(memberDetail == null ? null : memberDetail.getMember(), requestDTO.token());
        return YongJiResponse.success("FCM 토큰 비활성화 성공");
    }

    @Operation(summary = "로그인 기기 연결 해제", description = "현재 회원 소유의 FCM 토큰 연결을 해제합니다.")
    @PostMapping("/fcm-token/unbind")
    public ResponseEntity<YongJiResponse<String>> unbindFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @Valid @RequestBody FcmTokenRegisterRequestDTO requestDTO) {
        fcmTokenService.unbindToken(memberDetail.getMember(), requestDTO.token());
        return YongJiResponse.success("FCM 토큰 연결 해제 성공");
    }
    
    /** Legacy route; callers need to include the current device token to unbind only that device. */
    @Operation(summary = "현재 기기 FCM 연결 해제", description = "요청 본문이 있으면 현재 회원의 해당 토큰 연결을 해제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
        @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음", content = @Content)
    })
    @PostMapping("/fcm-token/remove")
    public ResponseEntity<YongJiResponse<String>> removeFcmToken(
            @AuthenticationPrincipal MemberDetail memberDetail,
            @Valid @RequestBody(required = false) FcmTokenRegisterRequestDTO requestDTO) {
        if (requestDTO != null) {
            fcmTokenService.unbindToken(memberDetail.getMember(), requestDTO.token());
        } else {
            fcmTokenService.unbindAllTokens(memberDetail.getMember());
        }
        return YongJiResponse.success("FCM 토큰 삭제 성공");
    }

    /**
     * 채팅방 퇴장 엔드포인트
     */
    @Operation(summary = "채팅방 퇴장", description = "참여 중인 채팅방에서 나갑니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 퇴장 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
        @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아닌 경우", content = @Content),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방", content = @Content)
    })
    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<YongJiResponse<String>> leaveChatRoom(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail,
            @Parameter(description = "퇴장할 채팅방 ID", required = true)
            @PathVariable("roomId") Long roomId) {
        
        chatService.leaveChatRoom(roomId, memberDetail.getMember());
        
        return YongJiResponse.success("채팅방 퇴장 성공");
    }
}
