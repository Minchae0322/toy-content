package com.example.toycontent.app.common.resolver;

import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
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
public class CurrentUserIsAdminArgumentResolver implements HandlerMethodArgumentResolver {

  private static final String ROLE_ADMIN = "ADMIN";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUserIsAdmin.class)
        && (parameter.getParameterType().equals(Boolean.class)
            || parameter.getParameterType().equals(boolean.class));
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return false;
    }

    return authentication.getAuthorities().stream()
        .anyMatch(grantedAuthority -> ROLE_ADMIN.equals(grantedAuthority.getAuthority()));
  }
}
