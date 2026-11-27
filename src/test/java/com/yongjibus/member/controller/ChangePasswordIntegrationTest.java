package com.yongjibus.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.yongjibus.global.infra.jwt.JwtService;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordIntegrationTest {

    private static final String EMAIL = "pwchange@mju.ac.kr";
    private static final String CURRENT_PASSWORD = "original-password";
    private static final String NEW_PASSWORD = "replacement-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.builder()
                .email(EMAIL)
                .name("테스터")
                .username("pwchange")
                .password(passwordEncoder.encode(CURRENT_PASSWORD))
                .build());
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteById(member.getId());
        jwtService.deleteRefreshToken(EMAIL);
    }

    @Test
    void changePasswordRequiresAnAccessToken() throws Exception {
        mockMvc.perform(put("/member/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordChangeBody(CURRENT_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void wrongCurrentPasswordLeavesCredentialsUnchanged() throws Exception {
        changePassword("incorrect-password", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data").value("현재 비밀번호가 올바르지 않습니다."));

        login(CURRENT_PASSWORD).andExpect(status().isOk());
        login(NEW_PASSWORD).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "        ", "123456789012345678901"})
    void newPasswordMustFollowSignupPolicy(String newPassword) throws Exception {
        changePassword(CURRENT_PASSWORD, newPassword).andExpect(status().isBadRequest());
        login(CURRENT_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void changePasswordPersistsNewCredentialsAndPreservesSession() throws Exception {
        String refreshToken = jwtService.createAndSaveRefreshToken(EMAIL);

        changePassword(CURRENT_PASSWORD, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("비밀번호가 변경되었습니다."));

        mockMvc.perform(post("/auth/token/refresh").header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isOk());
        login(CURRENT_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
    }

    private ResultActions changePassword(String currentPassword, String newPassword) throws Exception {
        return mockMvc.perform(put("/member/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createAccessToken(EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordChangeBody(currentPassword, newPassword)));
    }

    private String passwordChangeBody(String currentPassword, String newPassword) {
        return """
                {"currentPassword":"%s","newPassword":"%s"}
                """.formatted(currentPassword, newPassword);
    }

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(EMAIL, password)));
    }
}
