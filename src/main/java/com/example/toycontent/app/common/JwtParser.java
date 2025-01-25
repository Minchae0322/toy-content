package com.example.toycontent.app.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

@Component
public class JwtParser {

    private final String secretKey = "your-secret-key"; // 변경: 환경 변수 또는 설정 파일로 관리

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class); // "userId" 클레임에 사용자 ID 저장
    }
}
