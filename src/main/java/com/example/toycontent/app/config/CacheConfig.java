package com.example.toycontent.app.config;

import java.time.Duration;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 핫리스트 공유 캐시 (Redis).
 *
 * <p>대상: 사용자 무관 + 갱신 주기가 긴(핫스코어 스케줄러 1시간) 목록 3종 -
 * feeds/hot · battles/hot · 인기 products. 실측 근거: 실트래픽 최다 호출(대시보드)이며
 * 요청당 CPU ~16ms가 처리량 상한을 정하고 있었다 (T2-B 314rps에서 RDS 커넥션 59/60).
 * 스크롤·상세·검색은 캐시하지 않는다 - 사용자별/실시간 경로.
 *
 * <p><b>왜 Redis인가 (처음 설계는 Caffeine 로컬이었다):</b> 스케줄러가 ShedLock으로 리더
 * 파드에서만 돌아, 로컬 캐시로는 갱신 직후 파드 간 순위가 최대 TTL(5분)만큼 어긋난다 -
 * 새로고침마다 순위가 왔다갔다 할 수 있는 이 불일치를 제품 요구로 불허하면서 전환 조건
 * "파드 간 즉시 정합 요구"가 발동됐다. Redis는 이미 의존 중(ShedLock·작성자 캐시)이라
 * 인프라 추가 0이고, 리더의 @CacheEvict 한 번으로 전 파드가 즉시 일관된다.
 * 대가: 히트당 네트워크 왕복(~1ms)과 JDK 직렬화 비용 - DB 재계산 16ms 대비 여전히 1/10 이하.
 *
 * <p>직렬화: JDK (RedisCache 기본). 캐시 대상 DTO는 전부 Serializable + serialVersionUID 고정.
 * 배포 간 클래스 비호환 시엔 {@link #errorHandler()}가 미스로 강등해 자가 치유한다(TTL 5분).
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

  public static final String HOT_FEEDS = "hotFeeds";
  public static final String HOT_BATTLES = "hotBattles";
  public static final String POPULAR_PRODUCTS = "popularProducts";

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration conf = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))   // 안전망 - 정상 무효화는 스케줄러 @CacheEvict가 담당
        .prefixCacheNameWith("hotlist:");
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(conf)
        .initialCacheNames(Set.of(HOT_FEEDS, HOT_BATTLES, POPULAR_PRODUCTS))
        .enableStatistics()                // cache_gets{result=hit|miss} - 적중률 실측용
        .build();
  }

  /**
   * 캐시 장애를 서비스 장애로 승격시키지 않는다 - Redis 다운·역직렬화 실패(배포 간 클래스
   * 변경)는 전부 "캐시 미스"로 강등하고 DB 경로로 폴백한다. 캐시는 최적화지 의존성이 아니다.
   */
  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
        log.warn("[cache] GET 실패 - 미스로 폴백: cache={}, key={}, cause={}", cache.getName(), key, e.toString());
      }
      @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
        log.warn("[cache] PUT 실패 - 무시: cache={}, key={}, cause={}", cache.getName(), key, e.toString());
      }
      @Override public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
        log.warn("[cache] EVICT 실패: cache={}, key={}, cause={}", cache.getName(), key, e.toString());
      }
      @Override public void handleCacheClearError(RuntimeException e, Cache cache) {
        log.warn("[cache] CLEAR 실패: cache={}, cause={}", cache.getName(), e.toString());
      }
    };
  }
}
