package com.yongjibus.global.config.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongjibus.auth.service.MemberDetailService;
import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.AuthException;
import com.yongjibus.global.infra.jwt.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MemberDetailService memberDetailService;

    private static final Set<String> EXCLUDED_ENDPOINTS = Set.of(
        "/ws-stomp",
        "/auth",
        "/actuator",
        "/vacation",
        "/day",
        "/arrivaltime"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI(); 
        try {
            // 토큰 재발급 요청시
            if (path.equals("/auth/token/refresh")) {

                String refreshToken = jwtService.extractToken(request)
                    .orElseThrow(() -> new AuthException(ErrorCode.INVALID_REFRESH_TOKEN));
                
                if (jwtService.validateRefreshToken(refreshToken)) {
                    String email = jwtService.getEmailFromToken(refreshToken);
                    UserDetails memberDetail = memberDetailService.loadUserByUsername(email);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(memberDetail, refreshToken, memberDetail.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    throw new AuthException(ErrorCode.INVALID_REFRESH_TOKEN);
                }
            }

            if (EXCLUDED_ENDPOINTS.stream().anyMatch(path::startsWith) && !path.startsWith("/auth/logout") && !path.startsWith("/auth/signout")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 1. 토큰 유효성 검증
            String accessToken = jwtService.extractToken(request)
                .orElseThrow(() -> new AuthException(ErrorCode.INVALID_ACCESS_TOKEN));

            // 2. 토큰 유효성 검증
            if (!jwtService.validateAccessToken(accessToken)) {
                throw new AuthException(ErrorCode.EXPIRED_ACCESS_TOKEN);
            }

            // 3. 토큰 파싱
            String email = jwtService.getEmailFromToken(accessToken);
            if (email != null) {
                UserDetails memberDetail = memberDetailService.loadUserByUsername(email);
                Authentication authentication = new UsernamePasswordAuthenticationToken(memberDetail, null, memberDetail.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            sendErrorResponse(response, e.getErrorCode());
            return;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {

        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", errorCode.name());
        errorDetails.put("message", errorCode.getMessage());
        errorDetails.put("status", errorCode.getStatus());
        
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorDetails));
    }
}
