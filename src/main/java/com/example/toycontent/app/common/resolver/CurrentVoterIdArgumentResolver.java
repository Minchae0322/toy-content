package com.example.toycontent.app.common.resolver;

import com.example.toycontent.app.auth.CustomUserDetails;
import com.example.toycontent.app.common.annotation.CurrentVoterId;
import com.example.toycontent.app.common.voter.VoterId;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @CurrentVoterId} 파라미터를 {@link VoterId}로 바인딩한다.
 * 로그인 사용자면 user, 비로그인이면 게스트 쿠키(gid) 기반 guest로 폴백.
 */
@Component
public class CurrentVoterIdArgumentResolver implements HandlerMethodArgumentResolver {

  // 비로그인 투표자 식별용 쿠키. 1년 유지하여 같은 브라우저의 중복 투표를 차단.
  static final String GUEST_COOKIE_NAME = "gid";
  private static final Duration GUEST_COOKIE_TTL = Duration.ofDays(365);

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentVoterId.class)
        && parameter.getParameterType().equals(VoterId.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    // 인증 우선, 실패 시 게스트로 폴백
    Long userId = authenticatedUserId();
    if (userId != null) {
      return VoterId.user(userId);
    }
    return VoterId.guest(resolveGuestId(webRequest));
  }

  private Long authenticatedUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    // 익명 사용자는 principal이 String("anonymousUser")이라 instanceof로 자연 제외됨
    if (auth.getPrincipal() instanceof CustomUserDetails principal) {
      return principal.getUserId();
    }
    return null;
  }

  // 기존 쿠키가 있으면 재사용, 없으면 새로 발급하여 응답에 Set-Cookie로 내려준다
  private String resolveGuestId(NativeWebRequest webRequest) {
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    String existing = readGuestCookie(request);
    if (existing != null) {
      return existing;
    }
    String issued = UUID.randomUUID().toString();
    writeGuestCookie(request, webRequest.getNativeResponse(HttpServletResponse.class), issued);
    return issued;
  }

  private String readGuestCookie(HttpServletRequest request) {
    if (request == null || request.getCookies() == null) {
      return null;
    }
    // 동일 이름 쿠키가 여러 개일 수 있으므로 비어있지 않은 첫 값을 사용
    return Arrays.stream(request.getCookies())
        .filter(c -> GUEST_COOKIE_NAME.equals(c.getName()))
        .map(Cookie::getValue)
        .filter(v -> v != null && !v.isBlank())
        .findFirst()
        .orElse(null);
  }

  private void writeGuestCookie(HttpServletRequest request, HttpServletResponse response,
      String guestId) {
    if (response == null) {
      return;
    }
    // HttpOnly로 JS 접근 차단, SameSite=Lax로 CSRF 완화, Secure는 HTTPS 환경에서만 설정
    ResponseCookie cookie = ResponseCookie.from(GUEST_COOKIE_NAME, guestId)
        .httpOnly(true)
        .secure(request != null && request.isSecure())
        .sameSite("Lax")
        .path("/")
        .maxAge(GUEST_COOKIE_TTL)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
