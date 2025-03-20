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
import com.yongjibus.global.common.response.ApiResponse;

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

    /**
     * 사용자 이름(username) 중복 여부를 확인합니다.
     */
    @GetMapping("/username/{username}/exists")
    public ResponseEntity<ApiResponse<UsernameCheckResponseDTO>> checkUsernameExists(
            @PathVariable String username) {
        boolean exists = authService.checkUsernameExists(username);
        return ApiResponse.success(new UsernameCheckResponseDTO(exists));
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
     * @return 로그아웃 성공 메시지
     */
    @DeleteMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal MemberDetail memberDetail) {
        authService.logout(memberDetail.getMember());
        return ApiResponse.success("로그아웃이 완료되었습니다.");
    }
    
    /**
     * 회원 탈퇴를 처리합니다.
     * 회원 상태를 삭제됨으로 변경하고 저장합니다.
     * 
     * @return 회원 탈퇴 성공 메시지
     */
    @DeleteMapping("/signout")
    public ResponseEntity<ApiResponse<String>> signoutMember(@AuthenticationPrincipal MemberDetail memberDetail) {
        authService.signoutMember(memberDetail.getMember());
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }
    
}