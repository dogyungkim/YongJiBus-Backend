package com.yongjibus.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.chat.controller.dto.ChatMessageResponseDTO;
import com.yongjibus.chat.domain.ChatRoom;
import com.yongjibus.chat.domain.FCMToken;
import com.yongjibus.chat.service.ChatService;
import com.yongjibus.chat.service.FCMTokenService;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.ChatException;
import com.yongjibus.member.domain.Member;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest extends ControllerTestSupport {

    @Mock
    private ChatService chatService;

    @Mock
    private FCMTokenService fcmTokenService;

    private MockMvc mockMvc;
    private Member member;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new ChatController(chatService, fcmTokenService));
        member = Member.builder()
                .id(1L)
                .name("테스트")
                .username("tester")
                .email("tester@mju.ac.kr")
                .password("password123")
                .build();
        authentication = new UsernamePasswordAuthenticationToken(
                new MemberDetail(member),
                "access-token",
                new MemberDetail(member).getAuthorities()
        );
        authenticate(authentication);
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    @DisplayName("참여 가능한 채팅방 목록은 현재 사용자를 기준으로 응답한다")
    void getAvailableChatRooms_ShouldReturnRoomList() throws Exception {
        ChatRoom chatRoom = ChatRoom.builder()
                .name("등교 채팅방")
                .departureTime(LocalTime.of(8, 30))
                .build();
        ReflectionTestUtils.setField(chatRoom, "id", 1L);
        ReflectionTestUtils.setField(chatRoom, "createdAt", LocalDateTime.of(2026, 3, 30, 8, 0));
        chatRoom.addMember(member);

        when(chatService.getNotJoinedChatRooms(member)).thenReturn(List.of(chatRoom));

        mockMvc.perform(get("/chat/rooms").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("등교 채팅방"));

        verify(chatService).getNotJoinedChatRooms(member);
    }

    @Test
    @DisplayName("채팅방 생성은 요청 본문과 현재 사용자를 서비스에 전달한다")
    void createChatRoom_ShouldDelegateWithAuthenticatedMember() throws Exception {
        ChatRoom chatRoom = ChatRoom.builder()
                .name("새 채팅방")
                .departureTime(LocalTime.of(9, 0))
                .build();
        ReflectionTestUtils.setField(chatRoom, "id", 3L);
        ReflectionTestUtils.setField(chatRoom, "createdAt", LocalDateTime.of(2026, 3, 30, 9, 0));
        chatRoom.addMember(member);

        when(chatService.createChatRoom("새 채팅방", LocalTime.of(9, 0), member)).thenReturn(chatRoom);

        mockMvc.perform(post("/chat/rooms")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"새 채팅방",
                                  "departureTime":"09:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.name").value("새 채팅방"));

        verify(chatService).createChatRoom("새 채팅방", LocalTime.of(9, 0), member);
    }

    @Test
    @DisplayName("채팅방 생성 요청 검증 실패는 400 응답으로 감싼다")
    void createChatRoom_WhenValidationFails_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/chat/rooms")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":" ",
                                  "departureTime":"09:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("FCM 토큰 공개 등록은 토큰만 전달한다")
    void registerFcmToken_ShouldDelegateToService() throws Exception {
        clearAuthentication();
        mockMvc.perform(post("/chat/fcm-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"fcm-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("FCM 토큰 등록 성공"));

        verify(fcmTokenService).registerAnonymousToken("fcm-token");
    }

    @Test
    void legacyAuthenticatedRegistrationStillBindsTheMember() throws Exception {
        mockMvc.perform(post("/chat/fcm-token")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"fcm-token\"}"))
                .andExpect(status().isOk());

        verify(fcmTokenService).bindToken(member, "fcm-token");
    }

    @Test
    void legacyBodylessRemovalUnbindsTheMember() throws Exception {
        mockMvc.perform(post("/chat/fcm-token/remove").principal(authentication))
                .andExpect(status().isOk());

        verify(fcmTokenService).unbindAllTokens(member);
    }

    @Test
    void deactivateFcmToken_UsesCurrentMemberOrGuest() throws Exception {
        mockMvc.perform(post("/chat/fcm-token/deactivate")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"fcm-token\"}"))
                .andExpect(status().isOk());
        verify(fcmTokenService).deactivateToken(member, "fcm-token");

        clearAuthentication();
        mockMvc.perform(post("/chat/fcm-token/deactivate")
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"guest-token\"}"))
                .andExpect(status().isOk());
        verify(fcmTokenService).deactivateToken(null, "guest-token");
    }

    @Test
    @DisplayName("로그인 사용자는 본인 토큰만 bind 또는 unbind 요청한다")
    void bindAndUnbindFcmToken_ShouldUseAuthenticatedMember() throws Exception {
        mockMvc.perform(post("/chat/fcm-token/bind")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"fcm-token\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/chat/fcm-token/unbind")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"fcm-token\"}"))
                .andExpect(status().isOk());

        verify(fcmTokenService).bindToken(member, "fcm-token");
        verify(fcmTokenService).unbindToken(member, "fcm-token");
    }

    @Test
    @DisplayName("다른 회원이 소유한 토큰 bind는 409를 반환한다")
    void bindFcmToken_WhenOwnedByAnotherMember_ShouldReturnConflict() throws Exception {
        doThrow(new ChatException(ErrorCode.FCM_TOKEN_CONFLICT))
                .when(fcmTokenService).bindToken(member, "fcm-token");

        mockMvc.perform(post("/chat/fcm-token/bind")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("{\"token\":\"fcm-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
