package com.example.toycontent.app.common.resolver;

import com.example.toycontent.app.auth.CustomUserDetails;
import com.example.toycontent.app.common.annotation.CurrentUserName;
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
public class CurrentUserNameArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUserName.class)
        && parameter.getParameterType().equals(Long.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("인증되지 않은 사용자입니다.");
    }

    CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

    if (principal.getUsername() != null) {
      return principal.getUsername();
    }

    throw new IllegalStateException("인증 정보에서 사용자 명을 찾을 수 없습니다.");
  }
}
