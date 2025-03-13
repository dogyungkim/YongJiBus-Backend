package com.yongjibus.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.dto.EmailAuthCodeRequestDTO;
import com.yongjibus.auth.domain.dto.EmailVerifyRequestDTO;
import com.yongjibus.auth.domain.dto.LoginRequestDTO;
import com.yongjibus.auth.domain.dto.SignupRequestDTO;
import com.yongjibus.auth.domain.MemberDetail;
import com.yongjibus.auth.domain.dto.AuthTokenDTO;
import com.yongjibus.auth.service.AuthService;
import com.yongjibus.global.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<String>> sendAuthEmail(@Valid @RequestBody EmailAuthCodeRequestDTO codeRequestDTO) {
        authService.sendAuthEmail(codeRequestDTO.email());
        return ApiResponse.success("인증 코드가 이메일로 전송되었습니다.");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<String>> verifyAuthCode(@Valid @RequestBody EmailVerifyRequestDTO dto) {
        
        authService.verifyAuthCode(dto.email(), dto.authCode());
        return ApiResponse.success("인증 코드가 확인되었습니다.");
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthTokenDTO>> signup(@Valid @RequestBody SignupRequestDTO dto) {

        List<String> tokens = authService.signup(dto.toEntity());
        
        return ApiResponse.success(new AuthTokenDTO(tokens.get(0), tokens.get(1)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenDTO>> login(@Valid @RequestBody LoginRequestDTO dto) {
        AuthTokenDTO tokenResponse = authService.login(dto.email(), dto.password());
        return ApiResponse.success(tokenResponse);
    }
    
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AuthTokenDTO>> refreshAccessToken(@AuthenticationPrincipal MemberDetail memberDetail, Authentication authentication) {
        AuthTokenDTO tokenResponse = authService.refreshAccessToken(authentication.getCredentials().toString(), memberDetail.getMember());
        return ApiResponse.success(tokenResponse);
    }
    
    // @PostMapping("/token/rotate")
    // public ResponseEntity<ApiResponse<AuthTokenDTO>> refreshAllTokens(@Valid @RequestBody TokenRefreshRequestDTO dto) {
    //     AuthTokenDTO tokenResponse = authService.refreshAllTokens(dto.refreshToken());
    //     return ApiResponse.success(tokenResponse);
    // }
    
    /**
     * 사용자 로그아웃을 처리합니다.
     * RefreshToken을 무효화하여 로그아웃 처리합니다.
     * 
     * @param dto 로그아웃 요청 DTO (RefreshToken 포함)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal MemberDetail memberDetail) {
        authService.logout(memberDetail.getMember());
        return ApiResponse.success("로그아웃이 완료되었습니다.");
    }
    
}