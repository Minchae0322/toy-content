package com.example.toycontent.external.user.service;


import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.UserErrorCode;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  private final WebClient userServiceWebClient;
  private final UserInfoCacheService userInfoCacheService;

  /**
   * 여러 사용자 정보 일괄 조회 (List 반환)
   */
  public List<ExternalUserInfo> getUserInfosOrElseCache(List<Long> userIds) {
    if (ObjectUtils.isEmpty(userIds)) {
      return List.of();
    }

    return userIds.stream()
        .map(this::getUserInfoOrElseCache)
        .toList();
  }

  public ExternalUserInfo getUserInfoOrElseCache(Long userId) {
    if (userId == null || userId <= 0) {
      throw new RestApiException(UserErrorCode.USER_NOT_INVALID);
    }

    return userInfoCacheService.getCachedUserInfos(userId)
        .orElseGet(() -> fetchAndCacheUserInfo(userId));
  }

  /**
   * 사용자 닉네임만 조회 (간단한 버전)
   */
  public String getUserNickname(Long userId) {
    ExternalUserInfo externalUserInfo = getUserInfoOrElseCache(userId);
    return externalUserInfo != null ? externalUserInfo.getNickname() : "사용자" + userId;
  }

  /**
   * 캐시 무효화 (단일 사용자)
   */
  public void invalidateUserCache(Long userId) {
    userInfoCacheService.evictUserCache(userId);
  }

  /**
   * 캐시 무효화 (여러 사용자)
   */
  public void invalidateUserCacheBatch(List<Long> userIds) {
    userInfoCacheService.evictUserCacheBatch(userIds);
  }

  /**
   * 전체 사용자 캐시 무효화 (패턴 기반)
   */
  public void invalidateAllUserCache() {
    userInfoCacheService.evictUserCacheByPattern("*");
  }

  /**
   * 외부 서비스에서 사용자 정보 조회 후 캐시 저장
   */
  private ExternalUserInfo fetchAndCacheUserInfo(Long userId) {
    ExternalUserInfo externalUserInfo = userServiceWebClient.get()
        .uri("/api/external/users/{userId}", userId)
        .retrieve()
        .onStatus(
            status -> status.value() == 404,
            response -> Mono.error(new RestApiException(UserErrorCode.USER_NOT_EXIST))
        )
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> Mono.error(new RestApiException(UserErrorCode.LOGIN_FAILED))
        )
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(new RestApiException(UserErrorCode.USER_SERVICE_ERROR))
        )
        .bodyToMono(ExternalUserInfo.class)
        .timeout(TIMEOUT)
        .doOnError(error -> log.error("[User service] 서비스 호출 실패: userId={}, error={}", userId,
            error.getMessage()))
        .block();

    if (externalUserInfo == null) {
      throw new RestApiException(UserErrorCode.USER_NOT_EXIST);
    }

    boolean success = userInfoCacheService.cacheUserInfo(externalUserInfo);
    return externalUserInfo;
  }

  /**
   * 외부 서비스에서 사용자 정보 일괄 조회
   */
  private Map<Long, ExternalUserInfo> fetchUserInfosFromService(List<Long> userIds) {
    try {
      String userIdsParam = String.join(",",
          userIds.stream().map(String::valueOf).toList());

      List<ExternalUserInfo> fetchedUsers = userServiceWebClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/api/users")
              .queryParam("ids", userIdsParam)
              .build())
          .retrieve()
          .bodyToFlux(ExternalUserInfo.class)
          .collectList()
          .timeout(TIMEOUT)
          .doOnError(error ->
              log.error("❌ 사용자 정보 일괄 조회 실패: userIds={}, error={}",
                  userIds, error.getMessage()))
          .onErrorReturn(List.of())
          .block();

      if (fetchedUsers == null) {
        fetchedUsers = List.of();
      }

      // List를 Map으로 변환
      Map<Long, ExternalUserInfo> result = fetchedUsers.stream()
          .filter(user -> user != null && user.getUserId() != null)
          .collect(Collectors.toMap(ExternalUserInfo::getUserId, user -> user));

      // 조회되지 않은 사용자들에 대해 폴백 데이터 생성
      userIds.forEach(userId -> {
        if (!result.containsKey(userId)) {
          result.put(userId, createFallbackUserInfo(userId));
        }
      });

      log.debug("👥 외부 서비스 일괄 조회 완료: 요청={}, 성공={}",
          userIds.size(), fetchedUsers.size());

      return result;

    } catch (Exception e) {
      log.error("❌ 외부 서비스 일괄 조회 중 예외 발생: userIds={}", userIds, e);
      return userIds.stream()
          .collect(Collectors.toMap(id -> id, this::createFallbackUserInfo));
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