package com.example.toycontent.app.auth.token;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.*;

@Component
public class JwtParser {

    private final Key key;

    public JwtParser(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    public boolean validateToken(String token) {
        try {
            // JWT 토큰 파싱 (유효한 서명 및 형식인지 검사)
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // JWT 서명이 올바르지 않거나, 토큰 형식이 잘못된 경우
            // ex) 위조된 토큰
        } catch (ExpiredJwtException e) {
            // 토큰이 만료된 경우
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 JWT 토큰인 경우
        } catch (IllegalArgumentException e) {
            // JWT 토큰이 비어있거나 null인 경우
        }
        return false;
    }



    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.get("username", String.class);
    }

    public List<String> getRoles(String token) {
        Claims claims = parseClaims(token);
        String authString = claims.get("auth", String.class);

        if (authString == null || authString.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(authString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }


    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims(); // 만료된 토큰에서도 Claims는 가져올 수 있음
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT Token", e);
        }
    }

    public String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
