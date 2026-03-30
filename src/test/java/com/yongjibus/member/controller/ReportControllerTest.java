package com.yongjibus.member.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.service.ReportService;
import com.yongjibus.support.ControllerTestSupport;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest extends ControllerTestSupport {

    @Mock
    private ReportService reportService;

    private MockMvc mockMvc;
    private Member member;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = createMockMvc(new ReportController(reportService));
        member = Member.builder()
                .id(1L)
                .name("신고자")
                .username("reporter")
                .email("reporter@mju.ac.kr")
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
    @DisplayName("신고 요청은 principal 기반 신고자 정보로 서비스에 위임된다")
    void reportUser_ShouldDelegateWithAuthenticatedReporter() throws Exception {
        mockMvc.perform(post("/report/user")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reportedUserName":"target-user",
                                  "reason":"욕설",
                                  "roomId":12
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("success"));

        verify(reportService).createReport(
                argThat(report -> report.getReporter().equals(member)
                        && "욕설".equals(report.getReason())
                        && Long.valueOf(12L).equals(report.getRoomId())),
                eq("target-user")
        );
    }
}
