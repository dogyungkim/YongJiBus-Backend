package com.yongjibus.place.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.member.domain.Member;
import com.yongjibus.member.domain.MemberRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceSecurityIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void publicPlacesCanBeReadWithoutToken() throws Exception {
        mockMvc.perform(get("/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void localImagesCanBeReadWithoutToken() throws Exception {
        mockMvc.perform(get("/place-images/550e8400-e29b-41d4-a716-446655440000.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void kakaoSearchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/places/kakao-search").param("query", "카페"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidBearerTokenOnPublicApiUsesTheCommonErrorEnvelope() throws Exception {
        mockMvc.perform(get("/places").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void ordinaryMemberCannotUseAdminApi() throws Exception {
        mockMvc.perform(get("/admin/places").with(user("user").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void operatorCanUseAdminApi() throws Exception {
        Member operator = Member.builder().name("운영자").username("operator").email("operator@m.ji")
                .password("password").role(MemberRole.OPERATOR).build();
        MemberDetail detail = new MemberDetail(operator);
        var auth = new UsernamePasswordAuthenticationToken(detail, null, detail.getAuthorities());

        mockMvc.perform(get("/admin/places").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
