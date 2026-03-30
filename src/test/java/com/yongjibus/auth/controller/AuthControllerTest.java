package com.yongjibus.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.auth.service.AuthService;
import com.yongjibus.member.domain.Member;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest extends ControllerTestSupport {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private Member member;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new AuthController(authService));
        member = Member.builder()
                .id(1L)
                .name("테스트")
                .username("tester")
                .email("tester@mju.ac.kr")
                .password("password123")
                .build();
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    @DisplayName("이메일 인증 코드 발송 요청은 성공 메시지를 반환한다")
    void sendAuthEmail_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/auth/email")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"tester@mju.ac.kr"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("인증 코드가 이메일로 전송되었습니다."));

        verify(authService).sendAuthEmail("tester@mju.ac.kr");
    }

    @Test
    @DisplayName("회원가입 요청은 access token과 refresh token을 함께 반환한다")
    void signup_ShouldReturnIssuedTokens() throws Exception {
        when(authService.signup(any())).thenReturn(List.of("access-token", "refresh-token"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"tester@mju.ac.kr",
                                  "password":"password123",
                                  "name":"테스트",
                                  "username":"tester"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("회원가입 요청 검증 실패는 400 응답으로 감싼다")
    void signup_WhenValidationFails_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"tester@mju.ac.kr",
                                  "password":"short",
                                  "name":"테스트",
                                  "username":"tester"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("토큰 재발급은 principal과 credentials를 사용해 서비스에 위임한다")
    void refreshAccessToken_ShouldUseAuthenticationPrincipalAndCredentials() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new MemberDetail(member),
                "refresh-token",
                new MemberDetail(member).getAuthorities()
        );
        authenticate(authentication);

        when(authService.refreshAccessToken("refresh-token", member))
                .thenReturn(List.of("new-access", "new-refresh"));

        mockMvc.perform(post("/auth/token/refresh").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));

        verify(authService).refreshAccessToken("refresh-token", member);
    }

    @Test
    @DisplayName("로그아웃은 현재 사용자로 서비스에 위임한다")
    void logout_ShouldDelegateWithAuthenticatedMember() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new MemberDetail(member),
                "refresh-token",
                new MemberDetail(member).getAuthorities()
        );
        authenticate(authentication);

        mockMvc.perform(delete("/auth/logout").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("로그아웃이 완료되었습니다."));

        verify(authService).logout(member);
    }
}
