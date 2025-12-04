package com.example.toycontent.external.user.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalUserFollowingService {

  private final FollowingCacheStore cacheStore;
  private final ExternalUserApiClient apiClient;

  /**
   * 사용자 팔로잉 목록 조회 (Cache-Aside 패턴)
   *
   * @param userId 사용자 ID
   * @return 팔로잉 사용자 ID List (절대 null 반환하지 않음)
   */
  public List<Long> getFollowingIds(Long userId) {
    if (userId == null) {
      return List.of();
    }

    return cacheStore.getFollowingIds(userId)
        .orElseGet(() -> fetchAndCache(userId));
  }

  /**
   * 특정 사용자를 팔로잉 중인지 확인
   */
  public boolean isFollowing(Long userId, Long targetUserId) {
    if (userId == null || targetUserId == null) {
      return false;
    }

    return cacheStore.isFollowing(userId, targetUserId)
        .orElseGet(() -> getFollowingIds(userId).contains(targetUserId));
  }

  /**
   * 팔로잉 캐시 무효화 (팔로우/언팔로우 이벤트 수신 시 호출)
   */
  public void evictCache(Long userId) {
    if (userId == null) {
      return;
    }

    if (cacheStore.evict(userId)) {
      log.info("팔로잉 캐시 무효화 완료: userId={}", userId);
    }
  }

  /**
   * 캐시 강제 갱신
   */
  public List<Long> refreshCache(Long userId) {
    if (userId == null) {
      return List.of();
    }

    cacheStore.evict(userId);
    return fetchAndCache(userId);
  }

  /**
   * API 호출 후 캐시 저장
   */
  private List<Long> fetchAndCache(Long userId) {
    try {
      List<Long> followingIds = apiClient.fetchFollowingIds(userId);
      cacheStore.cache(userId, followingIds);

      return followingIds;

    } catch (Exception e) {
      log.error("팔로잉 목록 조회 실패, Fallback 반환: userId={}", userId, e);
      return List.of();
    }
  }
}