package com.example.toycontent.external.user.service;

import com.example.toycontent.external.user.dto.ExternalUserInfo;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final UserServiceClient userServiceClient;

  private static final String USER_CACHE_PREFIX = "user:info:";
  private static final Duration CACHE_TTL = Duration.ofHours(24); // 24시간 TTL
  private static final Duration FALLBACK_TTL = Duration.ofMinutes(5); // 장애 시 짧은 TTL

  /**
   * 사용자 정보 조회 (캐시 우선, 실패 시 HTTP 호출)
   */
  public ExternalUserInfo getUserInfo(Long userId) {
    String cacheKey = USER_CACHE_PREFIX + userId;

    ExternalUserInfo cachedUser = getCachedUserInfo(cacheKey);
    if (cachedUser != null) {
      log.debug("Cache hit for userId: {}", userId);
      return cachedUser;
    }

    //캐시 미스 시 HTTP 호출
    log.debug("Cache miss for userId: {}, calling user service", userId);
    ExternalUserInfo externalUserInfo = userServiceClient.getUserInfoOrElseCache(userId);

    //조회 성공 시 캐시에 저장
    cacheUserInfo(cacheKey, externalUserInfo, CACHE_TTL);
    return externalUserInfo;
  }

  /**
   * 사용자 정보 캐시 무효화 (사용자 정보 변경 시 호출)
   */
  public void evictUserCache(Long userId) {
    String cacheKey = USER_CACHE_PREFIX + userId;
    redisTemplate.delete(cacheKey);
    log.info("Evicted cache for userId: {}", userId);
  }

  /**
   * 사용자 정보 캐시 갱신 (UserService에서 이벤트 받을 때)
   */
  public void refreshUserCache(Long userId, ExternalUserInfo externalUserInfo) {
    String cacheKey = USER_CACHE_PREFIX + userId;
    cacheUserInfo(cacheKey, externalUserInfo, CACHE_TTL);
    log.info("Refreshed cache for userId: {}", userId);
  }

  private ExternalUserInfo getCachedUserInfo(String cacheKey) {
    try {
      Object cached = redisTemplate.opsForValue().get(cacheKey);
      return cached instanceof ExternalUserInfo ? (ExternalUserInfo) cached : null;
    } catch (Exception e) {
      log.warn("Failed to get cache for key: {}", cacheKey, e);
      return null;
    }
  }

  private void cacheUserInfo(String cacheKey, ExternalUserInfo externalUserInfo, Duration ttl) {
    try {
      redisTemplate.opsForValue().set(cacheKey, externalUserInfo, ttl);
    } catch (Exception e) {
      log.warn("Failed to cache user info for key: {}", cacheKey, e);
    }
  }

  private ExternalUserInfo getExpiredCacheOrDefault(String cacheKey, Long userId) {
    // 만료된 캐시라도 있으면 반환 (긴급 상황 대응)
    try {
      Object cached = redisTemplate.opsForValue().get(cacheKey + ":backup");
      if (cached instanceof ExternalUserInfo externalUserInfo) {
        // 짧은 TTL로 다시 캐시 (서비스 복구 시 빠른 갱신)
        cacheUserInfo(cacheKey, externalUserInfo, FALLBACK_TTL);
        return externalUserInfo;
      }
    } catch (Exception e) {
      log.warn("Failed to get backup cache", e);
    }

    return createFallbackUserInfo(userId);
  }

  private ExternalUserInfo createFallbackUserInfo(Long userId) {
    return ExternalUserInfo.builder()
        .userId(userId)
        .nickname("사용자" + userId) // 기본 닉네임
        .profileImageFile(null)
        .build();
  }
}


