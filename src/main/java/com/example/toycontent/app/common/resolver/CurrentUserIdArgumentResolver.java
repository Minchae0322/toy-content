package com.example.toycontent.app.common.resolver;

import com.example.toycontent.app.auth.CustomUserDetails;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // required 속성 가져오기
        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = annotation != null && annotation.required();

        // 인증되지 않은 경우
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            if (required) {
                throw new IllegalStateException("인증되지 않은 사용자입니다.");
            }
            return null;  // required=false면 null 반환
        }

        // Principal 타입 체크
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            if (required) {
                throw new IllegalStateException("올바른 인증 정보가 아닙니다.");
            }
            return null;
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        if (principal.getUserId() != null) {
            return principal.getUserId();
        }

        // userId가 null인 경우
        if (required) {
            throw new IllegalStateException("인증 정보에서 사용자를 찾을 수 없습니다.");
        }

        return null;
    }
}