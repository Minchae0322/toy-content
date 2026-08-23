package com.example.toycontent.external.user.service;

import com.example.toycontent.external.user.dto.ExternalUserInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 사용자 정보 조회 통합 서비스
 *
 * 역할: Cache-Aside 패턴 구현 및 비즈니스 로직 처리
 * - UserInfoCacheService: Redis 캐시 CRUD
 * - UserServiceClient: 외부 UserService API 호출
 * - UserCacheService (현재 클래스): 둘을 조합하여 사용자에게 제공
 *
 * 사용 예시:
 * <pre>
 * // 단일 조회
 * ExternalUserInfo user = userCacheService.getUserInfo(userId);
 *
 * // 배치 조회
 * Map<Long, ExternalUserInfo> users = userCacheService.getUserInfos(userIds);
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalUserInfoService {

  private final UserCacheStore cacheService;  // Redis 캐시 레이어
  private final ExternalUserApiClient serviceClient;    // API 호출 레이어

  /**
   * 사용자 정보 조회 (캐시 우선, 미스 시 API 호출)
   *
   * 흐름:
   * 1. Redis 캐시 조회
   * 2. 캐시 HIT → 반환
   * 3. 캐시 MISS → UserService API 호출 → 캐시 저장 → 반환
   * 4. API 실패 → Fallback 데이터 반환
   *
   * @param userId 사용자 ID
   * @return 사용자 정보 (절대 null 반환하지 않음)
   */
  public ExternalUserInfo getUserInfo(Long userId) {
    if (userId == null) {
      log.info("[user-fallback] userId가 null 입니다. 대체 사용자 정보를 반환합니다.");
      return createFallbackUserInfo(null);
    }

    long start = System.currentTimeMillis();

    return cacheService.getCachedUserInfos(userId)
            .map(cachedInfo -> {
              long elapsed = System.currentTimeMillis() - start;
              log.info("[user-cache] 캐시 HIT - userId: {}, elapsed: {}ms", userId, elapsed);
              return cachedInfo;
            })
            .orElseGet(() -> {
              long elapsed = System.currentTimeMillis() - start;
              log.info("[user-cache] 캐시 MISS - userId: {}, elapsed: {}ms, API 호출합니다.", userId, elapsed);

              long apiStart = System.currentTimeMillis();
              ExternalUserInfo result = serviceClient.getUserInfoOrElseFetchAndCache(userId);
              long apiElapsed = System.currentTimeMillis() - apiStart;
              log.info("[user-fallback] API 호출 완료 - userId: {}, API elapsed: {}ms, total: {}ms",
                      userId, apiElapsed, System.currentTimeMillis() - start);
              return result;
            });
  }

  /**
   * 여러 사용자 정보 일괄 조회 (캐시 우선, 미스 시 API 배치 호출)
   * 흐름:
   * 1. Redis에서 일괄 조회 (multiGet)
   * 2. 캐시 미스된 ID만 추출
   * 3. UserService API 배치 호출
   * 4. 결과 병합 및 반환
   *
   * @param userIds 사용자 ID 목록
   * @return 사용자 ID -> 사용자 정보 맵
   */
  public Map<Long, ExternalUserInfo> getUserInfos(List<Long> userIds) {
    if (CollectionUtils.isEmpty(userIds)) {
      log.debug("[user-cache] 빈 요청 - 조회 생략");
      return Map.of();
    }

    // 유효한 ID 필터링
    List<Long> validUserIds = userIds.stream()
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (validUserIds.isEmpty()) {
      return Map.of();
    }

    // 캐시에서 일괄 조회 (MGET 왕복 1회 - ID당 순차 GET이던 것을 2026-08-23 배치화)
    Map<Long, ExternalUserInfo> cachedUsers = cacheService.getCachedUserInfoBatch(validUserIds);

    // 캐시 미스된 ID 추출
    List<Long> missingIds = validUserIds.stream()
        .filter(id -> !cachedUsers.containsKey(id))
        .toList();

    //  캐시 미스된 항목이 없으면 조기 반환
    if (missingIds.isEmpty()) {
      return cachedUsers;
    }

    //API 배치 호출 및 결과 병합
    Map<Long, ExternalUserInfo> fetchedUsers = serviceClient.fetchAndCacheUserInfos(missingIds);

    //캐시 조회 결과와 API 조회 결과 병합
    return Stream.of(cachedUsers, fetchedUsers)
        .flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));
  }

  /**
   * 사용자 닉네임만 조회 (경량화된 버전)
   *
   * @param userId 사용자 ID
   * @return 닉네임 (기본값: "사용자{userId}")
   */
  public String getUserNickname(Long userId) {
    ExternalUserInfo userInfo = getUserInfo(userId);
    return userInfo != null ? userInfo.getNickname() : "사용자" + userId;
  }

  /**
   * 여러 사용자 닉네임 일괄 조회
   *
   * @param userIds 사용자 ID 목록
   * @return 사용자 ID -> 닉네임 맵
   */
  public Map<Long, String> getUserNicknames(List<Long> userIds) {
    Map<Long, ExternalUserInfo> users = getUserInfos(userIds);

    Map<Long, String> nicknames = new HashMap<>();
    users.forEach((userId, userInfo) -> {
      String nickname = userInfo != null ? userInfo.getNickname() : "사용자" + userId;
      nicknames.put(userId, nickname);
    });

    return nicknames;
  }

  // ==================== Cache Management ====================

  /**
   * 사용자 정보 캐시 무효화 (사용자 정보 변경 시 호출)
   *
   * @param userId 사용자 ID
   */
  public void evictUserCache(Long userId) {
    cacheService.evictUserCache(userId);
    log.info("[user-cache] 사용자 캐시 무효화: userId={}", userId);
  }

  /**
   * 여러 사용자 캐시 일괄 무효화
   *
   * @param userIds 사용자 ID 목록
   * @return 삭제된 캐시 수
   */
  public long evictUserCacheBatch(List<Long> userIds) {
    long deletedCount = cacheService.evictUserCacheBatch(userIds);
    log.info("[user-cache] 사용자 캐시 일괄 무효화: 삭제={}", deletedCount);
    return deletedCount;
  }

  /**
   * 사용자 정보 캐시 강제 갱신 (외부 이벤트 수신 시)
   *
   * 사용 시나리오:
   * - Kafka/RabbitMQ 등에서 사용자 정보 변경 이벤트 수신
   * - UserService에서 Webhook 호출
   *
   * @param userId 사용자 ID
   * @param userInfo 갱신할 사용자 정보
   */
  public void refreshUserCache(Long userId, ExternalUserInfo userInfo) {
    if (userInfo == null || userId == null) {
      log.warn("[user-cache] 유효하지 않은 사용자 정보: userId={}", userId);
      return;
    }

    boolean success = cacheService.cacheUserInfo(userInfo);
    if (success) {
      log.info("[user-cache] 사용자 캐시 갱신 완료: userId={}", userId);
    } else {
      log.warn("[user-cache] 사용자 캐시 갱신 실패: userId={}", userId);
    }
  }

  /**
   * 패턴 기반 캐시 무효화
   * 주의: 프로덕션에서는 SCAN 명령어 사용 권장
   *
   * @param pattern 패턴 (예: "1*" → user:info:1로 시작하는 모든 키)
   * @return 삭제된 캐시 수
   */
  public long evictCacheByPattern(String pattern) {
    long deletedCount = cacheService.evictUserCacheByPattern(pattern);
    log.info("[user-cache] 패턴 기반 캐시 무효화: pattern={}, 삭제={}", pattern, deletedCount);
    return deletedCount;
  }

  // ==================== Monitoring & Debugging ====================

  /**
   * 캐시 존재 여부 확인
   *
   * @param userId 사용자 ID
   * @return 캐시 존재 여부
   */
  public boolean existsInCache(Long userId) {
    return cacheService.existsInCache(userId);
  }

  /**
   * 캐시 통계 정보 조회 (모니터링 용도)
   *
   * @return 캐시 통계 맵
   */
  public Map<String, Object> getCacheStats() {
    return cacheService.getCacheStats();
  }

  /**
   * 캐시 TTL 연장
   *
   * @param userId 사용자 ID
   * @param newTtl 새로운 TTL
   * @return 성공 여부
   */
  public boolean extendCacheTTL(Long userId, java.time.Duration newTtl) {
    return cacheService.extendCacheTTL(userId, newTtl);
  }

  // ==================== Private Utilities ====================

  /**
   * 폴백 사용자 정보 생성
   * API 장애 시 기본값 제공
   */
  private ExternalUserInfo createFallbackUserInfo(Long userId) {
    return ExternalUserInfo.builder()
        .userId(userId)
        .nickname("사용자" + (userId != null ? userId : "Unknown"))
        .email(null)
        .profileImageFile(null)
        .role("USER")
        .activated(true)
        .build();
  }
}