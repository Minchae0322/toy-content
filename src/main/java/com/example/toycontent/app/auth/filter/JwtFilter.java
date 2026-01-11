package com.example.toycontent.app.auth.filter;


import com.example.toycontent.app.auth.CustomUserDetails;
import com.example.toycontent.app.auth.token.JwtParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
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
        }

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
    }

    private boolean isOptionalAuthPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // GET /api/products, GET /api/products/{id} → 비로그인도 허용, 로그인 시 추가 정보 제공
        if ("GET".equals(method)) {
            return path.equals("/api/products") || path.matches("/api/products/\\d+");
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
