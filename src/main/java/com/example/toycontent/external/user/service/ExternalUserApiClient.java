package com.example.toycontent.external.user.service;


import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.UserErrorCode;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  /**
   * 여러 사용자 정보 일괄 조회 (List 반환)
   */
  public List<ExternalUserInfo> getUserInfosOrElseCache(List<Long> userIds) {
    if (ObjectUtils.isEmpty(userIds)) {
      return List.of();
    }

    return userIds.stream()
        .map(this::getUserInfoOrElseFetchAndCache)
        .toList();
  }

  public ExternalUserInfo getUserInfoOrElseFetchAndCache(Long userId) {
    if (userId == null || userId <= 0) {
      throw new RestApiException(UserErrorCode.USER_NOT_INVALID);
    }

    return userCacheStore.getCachedUserInfos(userId)
        .orElseGet(() -> {
          try {
            return fetchAndCacheUserInfo(userId);
          } catch (Exception e) {
            return createFallbackUserInfo(userId);
          }
        });
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
   * 외부 서비스에서 사용자 정보 조회 후 캐시 저장
   */
  private ExternalUserInfo fetchAndCacheUserInfo(Long userId) {
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
          .doOnError(error ->
              log.error("사용자 정보 조회 실패: userId={}", userId, error))
          .onErrorReturn(createFallbackUserInfo(userId))
          .block();

      if (externalUserInfo != null) {
        userCacheStore.cacheUserInfo(externalUserInfo);
        return externalUserInfo;
      }

      return createFallbackUserInfo(userId);

    } catch (Exception e) {
      log.error("외부 서비스 호출 중 예외: userId={}", userId, e);
      return createFallbackUserInfo(userId);
    }
  }

  /**
   * 팔로잉 목록 조회 API 호출
   */
  public List<Long> fetchFollowingIds(Long userId) {
    return userServiceWebClient.get()
        .uri("/api/external/users/{userId}/following/ids", userId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<Long>>() {})
        .timeout(Duration.ofSeconds(3))
        .map(list -> list.stream().distinct().toList())
        .onErrorReturn(List.of())
        .defaultIfEmpty(List.of())
        .block();
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