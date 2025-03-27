package com.yongjibus.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.controller.dto.EmailAuthCodeRequestDTO;
import com.yongjibus.auth.controller.dto.EmailVerifyRequestDTO;
import com.yongjibus.auth.controller.dto.LoginRequestDTO;
import com.yongjibus.auth.controller.dto.SignupRequestDTO;
import com.yongjibus.auth.controller.dto.UsernameCheckResponseDTO;
import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.auth.controller.dto.AuthTokenDTO;
import com.yongjibus.auth.service.AuthService;
import com.yongjibus.global.common.response.YongJiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 관리 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "이메일 인증 코드 발송", description = "MJU 이메일 주소로 인증 코드를 발송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식", content = @Content)
    })
    @PostMapping("/email")
    public ResponseEntity<YongJiResponse<String>> sendAuthEmail(
            @Parameter(description = "인증 코드를 받을 이메일 주소", required = true)
            @Valid @RequestBody EmailAuthCodeRequestDTO codeRequestDTO) {
        authService.sendAuthEmail(codeRequestDTO.email());
        return YongJiResponse.success("인증 코드가 이메일로 전송되었습니다.");
    }

    @Operation(summary = "이메일 인증 코드 확인", description = "발송된 인증 코드의 유효성을 검증합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 확인 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 인증 코드", content = @Content)
    })
    @PostMapping("/email/verify")
    public ResponseEntity<YongJiResponse<String>> verifyAuthCode(
            @Parameter(description = "이메일 및 인증 코드 정보", required = true)
            @Valid @RequestBody EmailVerifyRequestDTO dto) {
        
        authService.verifyAuthCode(dto.email(), dto.authCode());
        return YongJiResponse.success("인증 코드가 확인되었습니다.");
    }

    @Operation(summary = "사용자 이름 중복 확인", description = "입력한 사용자 이름(username)의 중복 여부를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "중복 확인 완료")
    })
    @GetMapping("/username/{username}/exists")
    public ResponseEntity<YongJiResponse<UsernameCheckResponseDTO>> checkUsernameExists(
            @Parameter(description = "확인할 사용자 이름", required = true)
            @PathVariable("username") String username) {
        boolean exists = authService.checkUsernameExists(username);
        return YongJiResponse.success(new UsernameCheckResponseDTO(exists));
    }

    @Operation(summary = "회원가입", description = "새로운 회원을 등록하고 JWT 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 회원 정보", content = @Content),
        @ApiResponse(responseCode = "409", description = "이메일 또는 사용자 이름 중복", content = @Content)
    })
    @PostMapping("/signup")
    public ResponseEntity<YongJiResponse<AuthTokenDTO>> signup(
            @Parameter(description = "회원가입 정보", required = true)
            @Valid @RequestBody SignupRequestDTO dto) {
        List<String> tokens = authService.signup(dto.toEntity());
        return YongJiResponse.success(new AuthTokenDTO(tokens.get(0), tokens.get(1)));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<YongJiResponse<AuthTokenDTO>> login(
            @Parameter(description = "로그인 정보", required = true)
            @Valid @RequestBody LoginRequestDTO dto) {
        List<String> tokens = authService.login(dto.email(), dto.password());
        AuthTokenDTO tokenResponse = new AuthTokenDTO(tokens.get(0), tokens.get(1));
        return YongJiResponse.success(tokenResponse);
    }
    
    @Operation(summary = "액세스 토큰 갱신", description = "RefreshToken을 사용하여 새로운 AccessToken을 발급받습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰", content = @Content)
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<YongJiResponse<AuthTokenDTO>> refreshAccessToken(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail, 
            @Parameter(description = "인증 정보", hidden = true)
            Authentication authentication) {
        List<String> tokens = authService.refreshAccessToken(authentication.getCredentials().toString(), memberDetail.getMember());
        AuthTokenDTO tokenResponse = new AuthTokenDTO(tokens.get(0), tokens.get(1));
        return YongJiResponse.success(tokenResponse);
    }
    
    @Operation(summary = "로그아웃", description = "RefreshToken을 무효화하여 로그아웃 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @DeleteMapping("/logout")
    public ResponseEntity<YongJiResponse<String>> logout(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail) {
        authService.logout(memberDetail.getMember());
        return YongJiResponse.success("로그아웃이 완료되었습니다.");
    }
    
    @Operation(summary = "회원 탈퇴", description = "회원 계정을 탈퇴 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content)
    })
    @DeleteMapping("/signout")
    public ResponseEntity<YongJiResponse<String>> signoutMember(
            @Parameter(description = "현재 인증된 사용자", hidden = true)
            @AuthenticationPrincipal MemberDetail memberDetail) {
        authService.signoutMember(memberDetail.getMember());
        return YongJiResponse.success("회원 탈퇴가 완료되었습니다.");
    }
}