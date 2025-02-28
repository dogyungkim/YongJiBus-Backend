package com.yongjibus.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongjibus.auth.domain.Member;
import com.yongjibus.auth.domain.dto.EmailAuthCodeRequestDTO;
import com.yongjibus.auth.domain.dto.EmailVerifyRequestDTO;
import com.yongjibus.auth.domain.dto.LoginRequestDTO;
import com.yongjibus.auth.domain.dto.SignupRequestDTO;
import com.yongjibus.auth.service.AuthService;
import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_USERNAME = "testuser";
    private final String TEST_NAME = "테스트유저";
    private final String TEST_AUTH_CODE = "123456";

    @Test
    @DisplayName("이메일 인증 코드 발송 API 테스트")
    void sendAuthEmailTest() throws Exception {
        // given
        EmailAuthCodeRequestDTO requestDTO = new EmailAuthCodeRequestDTO(TEST_EMAIL);
        doNothing().when(authService).sendAuthEmail(anyString());

        // when & then
        mockMvc.perform(post("/auth/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("인증 코드가 이메일로 전송되었습니다."));

        verify(authService).sendAuthEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 API 성공 테스트")
    void verifyAuthCodeSuccessTest() throws Exception {
        // given
        EmailVerifyRequestDTO requestDTO = new EmailVerifyRequestDTO(TEST_EMAIL, TEST_AUTH_CODE);
        doNothing().when(authService).verifyAuthCode(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/auth/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("인증 코드가 확인되었습니다."));

        verify(authService).verifyAuthCode(TEST_EMAIL, TEST_AUTH_CODE);
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 API 실패 테스트 - 잘못된 코드")
    void verifyAuthCodeFailTest() throws Exception {
        // given
        EmailVerifyRequestDTO requestDTO = new EmailVerifyRequestDTO(TEST_EMAIL, "wrong-code");
        willThrow(new AuthException(ErrorCode.INVALID_AUTH_CODE))
                .given(authService).verifyAuthCode(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/auth/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 API 성공 테스트")
    void signupSuccessTest() throws Exception {
        // given
        SignupRequestDTO requestDTO = new SignupRequestDTO(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME, TEST_NAME);
        List<String> tokens = Arrays.asList("access-token", "refresh-token");
        given(authService.signup(any(Member.class))).willReturn(tokens);

        // when & then
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("회원가입 API 실패 테스트 - 이메일 미인증")
    void signupFailEmailNotVerifiedTest() throws Exception {
        // given
        SignupRequestDTO requestDTO = new SignupRequestDTO(TEST_EMAIL, TEST_PASSWORD, TEST_USERNAME, TEST_NAME);
        willThrow(new AuthException(ErrorCode.EMAIL_NOT_VERIFIED))
                .given(authService).signup(any(Member.class));

        // when & then
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 API 성공 테스트")
    void loginSuccessTest() throws Exception {
        // given
        LoginRequestDTO requestDTO = new LoginRequestDTO(TEST_EMAIL, TEST_PASSWORD);
        doNothing().when(authService).login(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("로그인이 완료되었습니다."));

        verify(authService).login(TEST_EMAIL, TEST_PASSWORD);
    }

    @Test
    @DisplayName("로그인 API 실패 테스트 - 잘못된 인증 정보")
    void loginFailInvalidCredentialsTest() throws Exception {
        // given
        LoginRequestDTO requestDTO = new LoginRequestDTO(TEST_EMAIL, "wrong-password");
        willThrow(new AuthException(ErrorCode.INVALID_CREDENTIALS))
                .given(authService).login(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }
} 