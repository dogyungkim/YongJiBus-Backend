package com.yongjibus.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.member.controller.dto.MemberResponseDTO;
import com.yongjibus.member.domain.Member;
import com.yongjibus.global.common.response.ApiResponse;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
@Slf4j
public class MemberController {

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponseDTO>> getCurrentMember(
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        if (memberDetail == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }
        
        Member member = memberDetail.getMember();
        
        return ApiResponse.success(MemberResponseDTO.from(member));
    }
}