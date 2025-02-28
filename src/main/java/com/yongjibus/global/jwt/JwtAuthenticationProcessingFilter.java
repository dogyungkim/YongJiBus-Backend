package com.yongjibus.global.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.yongjibus.auth.service.MemberDetailService;
import com.yongjibus.global.exception.AuthException;
import com.yongjibus.global.exception.ErrorCode;

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
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/auth/") && !path.startsWith("/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 토큰 유효성 검증
        log.info("JwtAuthenticationProcessingFilter doFilterInternal");
        String accessToken = jwtService.extractToken(request)
            .orElse(null);

        // 2. 토큰 유효성 검증
        if (accessToken != null && !jwtService.validateToken(accessToken)) {
            throw new AuthException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        // 3. 토큰 만료 검증
        if (accessToken != null && jwtService.isTokenExpired(accessToken)) {
            throw new AuthException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        }

        // 4. 토큰 파싱
        String email = jwtService.getEmailFromToken(accessToken);
        log.info("email: {}", email);
        if (email != null) {
            UserDetails memberDetail = memberDetailService.loadUserByUsername(email);
            Authentication authentication = new UsernamePasswordAuthenticationToken(memberDetail, null, memberDetail.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
    
    
}
