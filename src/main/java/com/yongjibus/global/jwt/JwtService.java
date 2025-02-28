package com.yongjibus.global.jwt;

import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yongjibus.global.redis.JwtRedisService;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private Long accessTokenExpirationPeriod;

    @Value("${jwt.refresh-token-validity}")
    private Long refreshTokenExpirationPeriod;

    private SecretKey key;

    private final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private final String EMAIL_CLAIM = "email";

    private final String BEARER = "Bearer ";
    private final String ACCESS_HEADER = "Authorization";

    private final JwtRedisService jwtRedisService;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secretKey.getBytes();
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email) {
		Date now = new Date();

		return Jwts.builder()
            .setSubject(ACCESS_TOKEN_SUBJECT)
			.setExpiration(new Date(now.getTime() + accessTokenExpirationPeriod))
			.claim(EMAIL_CLAIM, email)
            .signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

    private String createRefreshToken() {
        Date now = new Date();

        return Jwts.builder()
            .setSubject(REFRESH_TOKEN_SUBJECT)
            .setExpiration(new Date(now.getTime() + refreshTokenExpirationPeriod))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
    
    /**
     * Refresh 토큰을 생성하고 Redis에 저장합니다.
     * 
     * @param email 사용자 이메일
     * @return 생성된 Refresh 토큰
     */
    public String createAndSaveRefreshToken(String email) {
        String refreshToken = createRefreshToken();
        jwtRedisService.setRefreshToken(email, refreshToken);
        return refreshToken;
    }
    
    /**
     * Redis에 저장된 Refresh 토큰을 검증합니다.
     * 
     * @param email 사용자 이메일
     * @param refreshToken 검증할 Refresh 토큰
     * @return 토큰 유효성 여부
     */
    public boolean validateRefreshToken(String email, String refreshToken) {
        String storedToken = jwtRedisService.getRefreshToken(email);
        return storedToken != null && storedToken.equals(refreshToken) && validateToken(refreshToken);
    }
    
    /**
     * Refresh 토큰을 갱신합니다.
     * 
     * @param email 사용자 이메일
     * @return 새로 생성된 Refresh 토큰
     */
    public String rotateRefreshToken(String email) {
        jwtRedisService.deleteRefreshToken(email);
        return createAndSaveRefreshToken(email);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getExpiration()
            .before(new Date());
    }

    public String getEmailFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get(EMAIL_CLAIM).toString();
    }
    
    public Optional<String> extractToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(ACCESS_HEADER))
			.filter(accessToken -> accessToken.startsWith(BEARER))
			.map(accessToken -> accessToken.replace(BEARER, ""));
	}
}