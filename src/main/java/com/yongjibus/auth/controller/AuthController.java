package com.yongjibus.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yongjibus.auth.domain.dto.EmailAuthCodeRequestDTO;
import com.yongjibus.auth.domain.dto.EmailVerifyRequestDTO;
import com.yongjibus.auth.domain.dto.LoginRequestDTO;
import com.yongjibus.auth.domain.dto.SignupRequestDTO;
import com.yongjibus.auth.service.AuthService;

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
    public ResponseEntity<String> sendAuthEmail(@Valid @RequestBody EmailAuthCodeRequestDTO codeRequestDTO) {
        authService.sendAuthEmail(codeRequestDTO.email());
        return ResponseEntity.ok("인증 코드가 이메일로 전송되었습니다.");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<String> verifyAuthCode(@Valid @RequestBody EmailVerifyRequestDTO dto) {
        
        if (authService.verifyAuthCode(dto.email(), dto.authCode())) {
            return ResponseEntity.ok("인증 코드가 확인되었습니다.");
        }
        
        return ResponseEntity.badRequest().body("인증 코드가 다릅니다.");
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDTO dto) {

        authService.signup(dto.toEntity());
        
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequestDTO dto) {
        authService.login(dto.email(), dto.password());
        return ResponseEntity.ok("로그인이 완료되었습니다.");
    }

}