package com.example.toycontent.app.common.aspect;

import com.example.toycontent.app.common.annotation.CheckAdmin;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class CheckUserPermissionAspect {

  @Before("@annotation(checkAdmin)")
  public void checkUserAdmin(CheckAdmin checkAdmin) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new RestApiException(UserErrorCode.UNAUTHORIZED);
    }

    boolean isAdmin = authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));

    if (!isAdmin) {
      throw new RestApiException(UserErrorCode.UNAUTHORIZED);
    }
  }
}