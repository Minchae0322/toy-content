package com.example.toycontent.app.auth.filter;


import com.example.toycontent.app.auth.CustomUserDetails;
import com.example.toycontent.app.auth.token.JwtParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID = "userId";

    private final JwtParser tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String token = tokenProvider.resolveAccessToken(request);

        // 토큰이 있으면 인증 처리
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserId(token);
            String userName = tokenProvider.getUsername(token);
            List<String> roles = tokenProvider.getRoles(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

            CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                userName,
                new ArrayList<>(authorities)
            );

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (userId != null) {
                MDC.put(MDC_USER_ID, String.valueOf(userId));
            }
        }

        try {
            // Optional 인증 경로는 토큰 없어도 통과
            if (isOptionalAuthPath(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 필수 인증 경로는 인증 정보 필요
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    private boolean isOptionalAuthPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("GET".equals(method)) {
            return // 상품 관련
                path.equals("/products")
                    || path.matches("/products/\\d+")
                    || path.matches("/products/\\d+/feeds")
                    || path.matches("/products/\\d+/battles")
                    || path.matches("/products/\\d+/reviews")
                    // 피드 관련
                    || path.equals("/feeds/scroll")
                    || path.equals("/feeds/list")
                    || path.matches("/feeds/\\d+")
                    // 피드 댓글
                    || path.matches("/feeds/\\d+/comments")
                    // 배틀 관련
                    || path.equals("/battles")
                    || path.equals("/battles/hot")
                    || path.matches("/battles/\\d+")
                    // 배틀 코멘트
                    || path.matches("/battles/\\d+/comments")
                    || path.matches("/battles/\\d+/items/\\d+/comments");
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/api/login")
            || path.startsWith("/api/oauth2")
            || path.startsWith("/swagger")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-resources/")
            || path.startsWith("/webjars/");
    }
}
