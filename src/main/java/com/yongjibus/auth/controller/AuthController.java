package com.yongjibus.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.dto.EmailAuthCodeRequestDTO;
import com.yongjibus.auth.domain.dto.EmailVerifyRequestDTO;
import com.yongjibus.auth.domain.dto.LoginRequestDTO;
import com.yongjibus.auth.domain.dto.SignupRequestDTO;
import com.yongjibus.auth.domain.dto.SignupResponseDTO;
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
    public ResponseEntity<ApiResponse<SignupResponseDTO>> signup(@Valid @RequestBody SignupRequestDTO dto) {

        List<String> tokens = authService.signup(dto.toEntity());
        
        return ApiResponse.success(new SignupResponseDTO(tokens.get(0), tokens.get(1)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequestDTO dto) {
        authService.login(dto.email(), dto.password());
        return ApiResponse.success("로그인이 완료되었습니다.");
    }
}