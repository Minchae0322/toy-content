package com.example.toycontent.external.user.service;

import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCacheStore {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  // 캐시 설정
  private static final String CACHE_KEY_PREFIX = "user:info:";
  private static final String USER_FOLLOWING_CACHE_KEY_PREFIX = "user:following:";
  private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
  private static final Duration FALLBACK_TTL = Duration.ofMinutes(2);
  private static final String FALLBACK_NICKNAME_PREFIX = "사용자";

  /**
   * 단일 사용자 정보 캐시 조회
   */
  public Optional<ExternalUserInfo> getCachedUserInfos(Long userId) {
    return Optional.ofNullable(userId)
        .map(this::buildCacheKey)
        .map(this::getCachedValue)
        .filter(StringUtils::hasText)
        .map(this::deserializeUserInfo);
  }

  private String getCachedValue(String cacheKey) {
    try {
      return redisTemplate.opsForValue().get(cacheKey);
    } catch (Exception e) {
      log.error("Redis 캐시 조회 실패: cacheKey={}", cacheKey, e);
      return null;
    }
  }

  /**
   * 단일 사용자 정보 캐시 저장
   */
  public boolean cacheUserInfo(ExternalUserInfo externalUserInfo) {
    return cacheUserInfo(externalUserInfo, DEFAULT_TTL);
  }

  /**
   * 단일 사용자 정보 캐시 저장 (TTL 지정)
   */
  public boolean cacheUserInfo(ExternalUserInfo externalUserInfo, Duration ttl) {
    if (!isValidUserInfo(externalUserInfo) || ttl == null || ttl.isNegative()) {
      return false;
    }

    try {
      String cacheKey = buildCacheKey(externalUserInfo.getUserId());
      String serializedValue = serializeUserInfo(externalUserInfo);

      redisTemplate.opsForValue().set(cacheKey, serializedValue, ttl);
      return true;

    } catch (Exception e) {
      log.error("Redis 캐시 저장 실패: userId={}", externalUserInfo.getUserId(), e);
      return false;
    }
  }

  /**
   * 여러 사용자 정보 일괄 캐시 저장
   */
  public int cacheUserInfoBatch(List<ExternalUserInfo> externalUserInfos) {
    return cacheUserInfoBatch(externalUserInfos, DEFAULT_TTL);
  }

  /**
   * 여러 사용자 정보 일괄 캐시 저장 (TTL 지정)
   */
  public int cacheUserInfoBatch(List<ExternalUserInfo> externalUserInfos, Duration ttl) {
    if (CollectionUtils.isEmpty(externalUserInfos) || ttl == null || ttl.isNegative()) {
      log.warn("⚠️ 유효하지 않은 사용자 정보 목록 또는 TTL");
      return 0;
    }

    List<ExternalUserInfo> validExternalUserInfos = externalUserInfos.stream()
        .filter(this::isValidUserInfo)
        .toList();

    if (validExternalUserInfos.isEmpty()) {
      log.warn("⚠️ 저장할 유효한 사용자 정보가 없습니다");
      return 0;
    }

    try {
      // Redis Pipeline을 사용하여 일괄 처리
      redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        validExternalUserInfos.forEach(externalUserInfo -> {
          try {
            String cacheKey = buildCacheKey(externalUserInfo.getUserId());
            String serializedValue = serializeUserInfo(externalUserInfo);
            connection.setEx(
                cacheKey.getBytes(),
                ttl.getSeconds(),
                serializedValue.getBytes()
            );
          } catch (Exception e) {
            log.warn("⚠️ 개별 사용자 정보 캐시 저장 실패: userId={}",
                externalUserInfo.getUserId(), e);
          }
        });
        return null;
      });

      log.debug("💾 사용자 정보 일괄 캐시 저장 완료: 성공={}/{}, ttl={}초",
          validExternalUserInfos.size(), externalUserInfos.size(), ttl.getSeconds());

      return validExternalUserInfos.size();

    } catch (Exception e) {
      log.error("❌ Redis 일괄 캐시 저장 실패", e);
      return 0;
    }
  }

  /**
   * 폴백 데이터 캐시 저장 (짧은 TTL)
   */
  public boolean cacheFallbackUserInfo(ExternalUserInfo fallbackExternalUserInfo) {
    if (!isValidUserInfo(fallbackExternalUserInfo)) {
      return false;
    }

    if (isFallbackUserInfo(fallbackExternalUserInfo)) {
      boolean result = cacheUserInfo(fallbackExternalUserInfo, FALLBACK_TTL);
      if (result) {
        log.debug("폴백 사용자 정보 캐시 저장: userId={}", fallbackExternalUserInfo.getUserId());
      }
      return result;
    }

    return false;
  }

  /**
   * 특정 사용자 캐시 무효화
   */
  public boolean evictUserCache(Long userId) {
    if (userId == null) {
      log.warn("⚠️ userId가 null입니다");
      return false;
    }

    try {
      String cacheKey = buildCacheKey(userId);
      Boolean deleted = redisTemplate.delete(cacheKey);
      boolean result = Boolean.TRUE.equals(deleted);

      log.debug("🗑️ 사용자 캐시 무효화: userId={}, deleted={}", userId, result);
      return result;

    } catch (Exception e) {
      log.error("❌ 사용자 캐시 무효화 실패: userId={}", userId, e);
      return false;
    }
  }

  /**
   * 여러 사용자 캐시 일괄 무효화
   */
  public long evictUserCacheBatch(List<Long> userIds) {
    if (CollectionUtils.isEmpty(userIds)) {
      return 0L;
    }

    List<Long> validUserIds = userIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

    if (validUserIds.isEmpty()) {
      log.warn("⚠️ 유효한 userId가 없습니다");
      return 0L;
    }

    try {
      List<String> cacheKeys = validUserIds.stream()
          .map(this::buildCacheKey)
          .collect(Collectors.toList());

      Long deletedCount = redisTemplate.delete(cacheKeys);
      long result = deletedCount != null ? deletedCount : 0L;

      log.debug("🗑️ 사용자 캐시 일괄 무효화: 요청={}, 삭제={}",
          validUserIds.size(), result);

      return result;

    } catch (Exception e) {
      log.error("❌ 사용자 캐시 일괄 무효화 실패: userIds={}", validUserIds, e);
      return 0L;
    }
  }

  /**
   * 패턴 기반 캐시 무효화 (주의해서 사용) 프로덕션 환경에서는 SCAN 사용 권장
   */
  public long evictUserCacheByPattern(String pattern) {
    if (!StringUtils.hasText(pattern)) {
      log.warn("⚠️ 패턴이 비어있습니다");
      return 0L;
    }

    try {
      String searchPattern = CACHE_KEY_PREFIX + pattern;
      Set<String> keys = redisTemplate.keys(searchPattern);

      if (CollectionUtils.isEmpty(keys)) {
        log.debug("🔍 패턴 매칭 키 없음: pattern={}", pattern);
        return 0L;
      }

      Long deletedCount = redisTemplate.delete(keys);
      long result = deletedCount != null ? deletedCount : 0L;

      log.debug("🗑️ 패턴 기반 캐시 무효화: pattern={}, 삭제={}",
          pattern, result);

      return result;

    } catch (Exception e) {
      log.error("❌ 패턴 기반 캐시 무효화 실패: pattern={}", pattern, e);
      return 0L;
    }
  }

  /**
   * 캐시 존재 여부 확인
   */
  public boolean existsInCache(Long userId) {
    if (userId == null) {
      return false;
    }

    try {
      String cacheKey = buildCacheKey(userId);
      return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
    } catch (Exception e) {
      log.error("❌ 캐시 존재 여부 확인 실패: userId={}", userId, e);
      return false;
    }
  }

  /**
   * 캐시 TTL 연장
   */
  public boolean extendCacheTTL(Long userId, Duration newTtl) {
    if (userId == null || newTtl == null || newTtl.isNegative()) {
      log.warn("⚠️ 유효하지 않은 파라미터: userId={}, ttl={}", userId, newTtl);
      return false;
    }

    try {
      String cacheKey = buildCacheKey(userId);
      boolean result = Boolean.TRUE.equals(redisTemplate.expire(cacheKey, newTtl));

      log.debug("⏰ 캐시 TTL 연장: userId={}, newTtl={}초, success={}",
          userId, newTtl.getSeconds(), result);

      return result;

    } catch (Exception e) {
      log.error("❌ 캐시 TTL 연장 실패: userId={}", userId, e);
      return false;
    }
  }

  /**
   * 캐시 통계 정보 조회 (개발/디버깅 용도)
   */
  public Map<String, Object> getCacheStats() {
    try {
      Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
      long totalKeys = keys != null ? keys.size() : 0;

      return Map.of(
          "totalCachedUsers", totalKeys,
          "cacheKeyPrefix", CACHE_KEY_PREFIX,
          "defaultTtlSeconds", DEFAULT_TTL.getSeconds(),
          "fallbackTtlSeconds", FALLBACK_TTL.getSeconds()
      );
    } catch (Exception e) {
      log.error("❌ 캐시 통계 조회 실패", e);
      return Map.of("error", e.getMessage());
    }
  }

  // === Private Helper Methods ===

  private String buildCacheKey(Long userId) {
    return CACHE_KEY_PREFIX + userId;
  }

  private String serializeUserInfo(ExternalUserInfo externalUserInfo) throws JsonProcessingException {
    return objectMapper.writeValueAsString(externalUserInfo);
  }

  private ExternalUserInfo deserializeUserInfo(String json) {
    try {
      return objectMapper.readValue(json, ExternalUserInfo.class);
    } catch (JsonProcessingException e) {
      log.error("UserInfo 역직렬화 실패: json={}", json, e);
      return null;
    }
  }

  private ExternalUserInfo parseUserInfoSafely(String cachedValue, Long userId) {
    if (!StringUtils.hasText(cachedValue)) {
      return null;
    }

    try {
      return deserializeUserInfo(cachedValue);
    } catch (Exception e) {
      log.warn("⚠️ 사용자 정보 역직렬화 실패: userId={}", userId, e);
      // 손상된 캐시 데이터 삭제
      evictUserCache(userId);
      return null;
    }
  }

  private boolean isValidUserInfo(ExternalUserInfo externalUserInfo) {
    return externalUserInfo != null
        && externalUserInfo.getUserId() != null
        && StringUtils.hasText(externalUserInfo.getNickname());
  }

  private boolean isFallbackUserInfo(ExternalUserInfo externalUserInfo) {
    return externalUserInfo != null
        && StringUtils.hasText(externalUserInfo.getNickname())
        && externalUserInfo.getNickname().startsWith(FALLBACK_NICKNAME_PREFIX);
  }
}
