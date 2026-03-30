package com.yongjibus.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.member.domain.Member;
import com.yongjibus.support.ControllerTestSupport;

class MemberControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private Member member;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new MemberController());
        member = Member.builder()
                .name("테스터")
                .username("tester")
                .email("tester@mju.ac.kr")
                .password("password123")
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);
        authentication = new UsernamePasswordAuthenticationToken(
                new MemberDetail(member),
                "access-token",
                new MemberDetail(member).getAuthorities()
        );
    }

    @AfterEach
    void tearDown() {
        clearAuthentication();
    }

    @Test
    @DisplayName("현재 회원 정보 조회는 principal 정보를 응답으로 변환한다")
    void getCurrentMember_ShouldReturnAuthenticatedMember() throws Exception {
        authenticate(authentication);

        mockMvc.perform(get("/member/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("tester@mju.ac.kr"))
                .andExpect(jsonPath("$.data.username").value("tester"));
    }

    @Test
    @DisplayName("principal이 없으면 인증 오류 응답을 반환한다")
    void getCurrentMember_WhenPrincipalIsMissing_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/member/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
