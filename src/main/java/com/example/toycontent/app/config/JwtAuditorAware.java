package com.example.toycontent.app.config;

import com.example.toycontent.app.common.JwtParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuditorAware implements AuditorAware<Long> {

    private final HttpServletRequest httpServletRequest;
    private final JwtParser jwtParser;


    @Override
    public Optional<Long> getCurrentAuditor() {
        // Authorization 헤더에서 JWT 토큰 추출
        String token = httpServletRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Optional.empty();
        }

        // "Bearer " 제거 후 토큰 파싱
        token = token.substring(7);

        try {
            return Optional.of(jwtParser.getUserId(token));
        } catch (Exception e) {
            return Optional.empty(); // 토큰이 유효하지 않을 경우
        }
    }
}
