package com.back.domain.member.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class AuthTokenService {

    private final SecretKey secretKey;
    private final long accessExpiration;

    public AuthTokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.accessExpiration = accessExpiration;
    }

    public String createAccessToken(Long memberId) {
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(memberId))   // 토큰을 발급받은 회원의 ID 저장
                .claim("tokenType", "access")   // 액세스 토큰과 리프레시 토큰을 구분
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long memberId) {
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("tokenType", "refresh")
                .issuedAt(now)
                // 리프레시 토큰에는 만료시간을 설정하지 않음
                .signWith(secretKey)
                .compact();
    }

    public Long getMemberId(String token) {
        Claims claims = getClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {               //  위조 여부와 액세스 토큰의 만료 여부 검증
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getClaims(token).get("tokenType"));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaims(token).get("tokenType"));
    }

    private Claims getClaims(String token) {
        return Jwts.parser()                  // 서버의 비밀키로 토큰 서명 검증
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}