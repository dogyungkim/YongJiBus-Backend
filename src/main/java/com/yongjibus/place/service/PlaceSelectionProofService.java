package com.yongjibus.place.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;
import com.yongjibus.place.client.KakaoLocalClient;
import com.yongjibus.place.client.KakaoLocalClient.KakaoPlace;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class PlaceSelectionProofService {
    private static final long VALIDITY_MILLIS = 5 * 60 * 1000;
    private static final String SUBJECT = "PlaceSelection";
    private final SecretKey key;

    public PlaceSelectionProofService(@Value("${nearby.selection-proof-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Long memberId, KakaoPlace place) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(SUBJECT)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + VALIDITY_MILLIS))
                .claim("memberId", memberId)
                .claim("placeId", place.id())
                .claim("placeUrl", place.placeUrl())
                .claim("roadAddress", place.roadAddressName())
                .claim("jibunAddress", place.addressName())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public SelectedPlace verify(String token, Long memberId) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            Number proofMemberId = (Number) claims.get("memberId");
            String placeId = claims.get("placeId", String.class);
            String placeUrl = KakaoLocalClient.normalizePlaceUrl(placeId, claims.get("placeUrl", String.class));
            String roadAddress = claims.get("roadAddress", String.class);
            String jibunAddress = claims.get("jibunAddress", String.class);
            if (!SUBJECT.equals(claims.getSubject()) || proofMemberId == null
                    || proofMemberId.longValue() != memberId || invalidAddresses(roadAddress, jibunAddress)) {
                throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
            }
            return new SelectedPlace(placeId, placeUrl, roadAddress, jibunAddress);
        } catch (JwtException | IllegalArgumentException | ClassCastException e) {
            throw new PlaceException(ErrorCode.INVALID_KAKAO_PLACE);
        }
    }

    private static boolean invalidAddresses(String roadAddress, String jibunAddress) {
        return ((roadAddress == null || roadAddress.isBlank()) && (jibunAddress == null || jibunAddress.isBlank()))
                || roadAddress != null && roadAddress.length() > 255
                || jibunAddress != null && jibunAddress.length() > 255;
    }

    public record SelectedPlace(String placeId, String placeUrl, String roadAddress, String jibunAddress) {
    }
}
