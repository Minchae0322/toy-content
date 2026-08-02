package com.example.toycontent.app.auth.token;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.*;

@Component
@Slf4j
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
        } catch (SignatureException e) {
            // 서명 불일치 — 위조 토큰 또는 서비스 간 시크릿 드리프트.
            // io.jsonwebtoken.security.SignatureException 이므로 io.jsonwebtoken.* 와일드카드로는
            // 잡히지 않는다. 과거 catch 절의 SecurityException 은 java.lang 것이어서 미포착 →
            // 필터 밖으로 전파 → 500 이었다.
            log.warn("[auth-jwt] JWT 서명 검증 실패 — 위조 또는 시크릿 불일치");
        } catch (ExpiredJwtException e) {
            log.warn("[auth-jwt] JWT 만료 exp={}", e.getClaims() == null ? "unknown" : e.getClaims().getExpiration());
        } catch (MalformedJwtException e) {
            log.warn("[auth-jwt] JWT 형식 오류");
        } catch (UnsupportedJwtException e) {
            log.warn("[auth-jwt] 지원하지 않는 JWT");
        } catch (IllegalArgumentException e) {
            log.warn("[auth-jwt] JWT 값이 비어 있음");
        } catch (JwtException e) {
            // 최후 방어. JwtException 은 jjwt 예외의 부모이고 io.jsonwebtoken 패키지라
            // 와일드카드에 잡힌다. 새 예외 타입이 생겨도 500 으로 새지 않는다.
            log.warn("[auth-jwt] JWT 검증 실패 {}", e.getClass().getSimpleName());
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
