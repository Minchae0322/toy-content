package com.example.toycontent.external.user.service;


import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.UserErrorCode;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalUserApiClient {
  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  private final WebClient userServiceWebClient;
  private final UserCacheStore userCacheStore;
  private final MeterRegistry meterRegistry;

  /**
   * 외부 서비스에서 여러 사용자 정보 일괄 조회 후 캐시 저장
   */
  public Map<Long, ExternalUserInfo> fetchAndCacheUserInfos(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<ExternalUserInfo> fetched = fetchUserInfosFromExternal(userIds);

    Map<Long, ExternalUserInfo> result = fetched.stream()
        .peek(userCacheStore::cacheUserInfo)
        .collect(Collectors.toMap(
            ExternalUserInfo::getUserId,
            Function.identity(),
            (existing, replacement) -> existing
        ));

    long fallbackCount = userIds.stream()
        .distinct()
        .filter(userId -> !result.containsKey(userId))
        .count();

    // 조회 실패한 userId는 fallback 처리
    userIds.forEach(userId ->
        result.computeIfAbsent(userId, this::createFallbackUserInfo));

    if (fallbackCount > 0) {
      // Prometheus에는 user_fallback_total로 노출된다 — 알람: rate(user_fallback_total[5m]) > 0
      meterRegistry.counter("user.fallback").increment(fallbackCount);
    }

    return result;
  }

  /**
   * 단건 조회 - 기존 로직 유지
   */
  public ExternalUserInfo getUserInfoOrElseFetchAndCache(Long userId) {
    if (userId == null || userId <= 0) {
      log.info("[외부사용자 조회] 유효하지 않은 userId: {}", userId);
      throw new RestApiException(UserErrorCode.USER_NOT_INVALID);
    }

    return userCacheStore.getCachedUserInfos(userId)
        .orElseGet(() -> {
          log.info("[외부사용자 조회] 캐시 미스 - userId: {}", userId);
          try {
            return fetchAndCacheUserInfo(userId);
          } catch (Exception e) {
            log.warn("[user-fallback] auth 단건 조회 실패 → 익명 대체: userId={}", userId, e);
            return createFallbackUserInfo(userId);
          }
        });
  }


  /**
   * 외부 서비스에서 사용자 정보 조회 후 캐시 저장
   */
  private ExternalUserInfo fetchAndCacheUserInfo(Long userId) {
    Map<String, String> callerMdc = MDC.getCopyOfContextMap();
    try {
      ExternalUserInfo externalUserInfo = userServiceWebClient.get()
          .uri("/api/external/users/{userId}", userId)
          .retrieve()
          .onStatus(
              HttpStatusCode::isError,
              response -> Mono.empty() // 예외 대신 빈 값
          )
          .bodyToMono(ExternalUserInfo.class)
          .timeout(TIMEOUT)
          .doOnError(error -> logWithCallerMdc(callerMdc, () ->
              log.error("[user-fallback] auth 단건 조회 실패: userId={}", userId, error)))
          .onErrorReturn(createFallbackUserInfo(userId))
          .block();

      if (externalUserInfo != null) {
        userCacheStore.cacheUserInfo(externalUserInfo);
        return externalUserInfo;
      }

      return createFallbackUserInfo(userId);

    } catch (Exception e) {
      log.error("[user-fallback] auth 호출 예외: userId={}", userId, e);
      return createFallbackUserInfo(userId);
    }
  }

  private List<ExternalUserInfo> fetchUserInfosFromExternal(List<Long> userIds) {
    String joinedIds = userIds.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));

    Map<String, String> callerMdc = MDC.getCopyOfContextMap();
    try {
      return userServiceWebClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/api/external/users")
              .queryParam("userIds", joinedIds)
              .build())
          .retrieve()
          .onStatus(HttpStatusCode::isError, response -> Mono.empty())
          .bodyToFlux(ExternalUserInfo.class)
          .timeout(TIMEOUT)
          .collectList()
          .doOnError(error -> logWithCallerMdc(callerMdc, () ->
              log.error("[user-fallback] auth 목록 조회 실패 → {}명 익명 대체: userIds={}",
                  userIds.size(), userIds, error)))
          .onErrorReturn(Collections.emptyList())
          .block();
    } catch (Exception e) {
      log.error("[user-fallback] auth 일괄 호출 예외: userIds={}", userIds, e);
      return Collections.emptyList();
    }
  }

  /**
   * 팔로잉 목록 조회 API 호출
   */
  public List<Long> fetchFollowingIds(Long userId) {
    return userServiceWebClient.get()
        .uri("/api/external/users/{userId}/followings", userId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Long>>() {})
        .timeout(Duration.ofSeconds(3))
        .map(list -> list.stream().distinct().toList())
        .onErrorReturn(List.of())
        .defaultIfEmpty(List.of())
        .block();
  }

  /**
   * 사용자 닉네임만 조회 (간단한 버전)
   */
  public String getUserNickname(Long userId) {
    ExternalUserInfo externalUserInfo = getUserInfoOrElseFetchAndCache(userId);
    return externalUserInfo != null ? externalUserInfo.getNickname() : "사용자" + userId;
  }

  /**
   * 캐시 무효화 (단일 사용자)
   */
  public void invalidateUserCache(Long userId) {
    userCacheStore.evictUserCache(userId);
  }

  /**
   * 캐시 무효화 (여러 사용자)
   */
  public void invalidateUserCacheBatch(List<Long> userIds) {
    userCacheStore.evictUserCacheBatch(userIds);
  }

  /**
   * 전체 사용자 캐시 무효화 (패턴 기반)
   */
  public void invalidateAllUserCache() {
    userCacheStore.evictUserCacheByPattern("*");
  }




  /**
   * WebClient 콜백은 Reactor Netty 이벤트 루프 스레드에서 실행돼 요청 스레드의
   * MDC(traceId·spanId·userId)가 비어 있다. 호출 시점에 캡처한 MDC를 복원한 뒤 로그를 남긴다.
   */
  private static void logWithCallerMdc(Map<String, String> callerMdc, Runnable logAction) {
    Map<String, String> backup = MDC.getCopyOfContextMap();
    if (callerMdc != null) {
      MDC.setContextMap(callerMdc);
    }
    try {
      logAction.run();
    } finally {
      if (backup != null) {
        MDC.setContextMap(backup);
      } else {
        MDC.clear();
      }
    }
  }

  /**
   * 폴백 사용자 정보 생성
   */
  private ExternalUserInfo createFallbackUserInfo(Long userId) {
    return ExternalUserInfo.builder()
        .userId(userId)
        .nickname("사용자" + userId)
        .email(null)
        .profileImageFile(null)
        .build();
  }
}