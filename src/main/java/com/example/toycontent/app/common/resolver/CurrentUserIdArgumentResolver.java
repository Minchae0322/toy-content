package com.example.toycontent.app.common.resolver;

import com.example.toycontent.app.auth.token.JwtParser;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtParser jwtParser;  // 직접 만든 JwtParser 주입

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = jwtParser.resolveAccessToken(request);

        if (!StringUtils.hasText(token) || !jwtParser.validateToken(token)) {
            throw new IllegalStateException("유효하지 않은 JWT 토큰입니다.");
        }

        // 어노테이션에서 클레임 키 이름 추출 (예: "userId", "memberId" 등)
        String claimName = Objects.requireNonNull(parameter.getParameterAnnotation(CurrentUserId.class)).value();
        Claims claims = jwtParser.parseClaims(token);

        Object claim = claims.get(claimName);

        if (claim instanceof Integer i) {
            return i.longValue();
        } else if (claim instanceof Long l) {
            return l;
        } else {
            throw new IllegalStateException("클레임 '" + claimName + "'은 Long 형식이 아닙니다.");
        }
    }
}

