package com.yongjibus.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.auth.service.AuthService;
import com.yongjibus.member.controller.dto.ChangePasswordRequestDTO;
import com.yongjibus.member.controller.dto.MemberResponseDTO;
import com.yongjibus.member.domain.Member;
import com.yongjibus.global.common.response.YongJiResponse;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
@Slf4j
@Tag(name = "회원 API", description = "회원 정보 조회 및 비밀번호 변경 API")
public class MemberController {

    private final AuthService authService;

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     */
    @Operation(
        summary = "현재 회원 정보 조회", 
        description = "현재 로그인한 사용자의 상세 정보를 조회합니다. 인증이 필요한 엔드포인트입니다.",
        security = { @SecurityRequirement(name = "Bearer Authentication") }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "회원 정보 조회 성공",
            content = @Content(
                schema = @Schema(implementation = MemberResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "인증되지 않은 사용자", 
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "접근 권한 없음", 
            content = @Content
        )
    })
    @GetMapping("/me")
    public ResponseEntity<YongJiResponse<MemberResponseDTO>> getCurrentMember(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail) {
        
        if (memberDetail == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }
        
        Member member = memberDetail.getMember();
        
        return YongJiResponse.success(MemberResponseDTO.from(member));
    }

    @Operation(summary = "비밀번호 변경", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 현재 비밀번호 불일치", content = @Content),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @PutMapping("/password")
    public ResponseEntity<YongJiResponse<String>> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal MemberDetail memberDetail,
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        if (memberDetail == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        authService.changePassword(memberDetail.getMember().getId(), request.currentPassword(), request.newPassword());
        return YongJiResponse.success("비밀번호가 변경되었습니다.");
    }
}
