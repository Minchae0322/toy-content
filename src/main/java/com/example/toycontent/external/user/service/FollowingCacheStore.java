package com.example.toycontent.external.user.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowingCacheStore {

  private final RedisTemplate<String, String> redisTemplate;

  private static final String KEY_PREFIX = "user:following:";
  private static final Duration TTL = Duration.ofMinutes(10);

  public Optional<List<Long>> getFollowingIds(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }

    try {
      String key = buildKey(userId);
      List<String> cached = redisTemplate.opsForList().range(key, 0, -1);

      if (CollectionUtils.isEmpty(cached)) {
        return Optional.empty();
      }

      List<Long> result = cached.stream()
          .map(Long::parseLong)
          .toList();

      return Optional.of(result);

    } catch (Exception e) {
      log.error("팔로잉 캐시 조회 실패: userId={}", userId, e);
      return Optional.empty();
    }
  }

  public boolean cache(Long userId, List<Long> followingIds) {
    if (userId == null) {
      return false;
    }

    try {
      String key = buildKey(userId);
      redisTemplate.delete(key);

      if (CollectionUtils.isEmpty(followingIds)) {
        return true;
      }

      String[] values = followingIds.stream()
          .map(String::valueOf)
          .toArray(String[]::new);

      redisTemplate.opsForList().rightPushAll(key, values);
      redisTemplate.expire(key, TTL);

      return true;

    } catch (Exception e) {
      log.error("팔로잉 캐시 저장 실패: userId={}", userId, e);
      return false;
    }
  }

  public Optional<Boolean> isFollowing(Long userId, Long targetUserId) {
    // List는 O(n) 조회라서 캐시 레벨에서 확인 안 함
    // 전체 목록 로드 후 contains 체크로 위임
    return Optional.empty();
  }

  public boolean evict(Long userId) {
    if (userId == null) {
      return false;
    }

    try {
      return Boolean.TRUE.equals(redisTemplate.delete(buildKey(userId)));
    } catch (Exception e) {
      log.error("팔로잉 캐시 무효화 실패: userId={}", userId, e);
      return false;
    }
  }

  private String buildKey(Long userId) {
    return KEY_PREFIX + userId;
  }
}