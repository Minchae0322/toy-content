package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedTrendingScheduler {

  private final FeedRepository feedRepository;

  @Value("${scheduler.feed-trending.threshold}")
  private Integer trendingThreshold;

  @Value("${scheduler.feed-trending.enabled}")
  private Boolean enabled;

  /**
   * 매일 자정: 트렌딩 상태 갱신 + 조회수 스냅샷
   */
  @Scheduled(cron = "${scheduler.feed-trending.cron}")
  @SchedulerLock(
      name = "productPopularity",
      lockAtLeastFor = "1h",
      lockAtMostFor = "6h"
  )
  @Transactional
  // 캐시 무효화 (2026-08-23): 재계산 완료 직후 Redis 공유 캐시를 비운다.
  // 캐시가 Redis(전 파드 공유)라, ShedLock 리더 1곳의 evict 한 번으로 모든 파드가 즉시
  // 새 목록을 본다 - 파드 간 순위 불일치 없음. TTL 5분은 evict 누락 대비 안전망.
  @org.springframework.cache.annotation.CacheEvict(
      cacheNames = com.example.toycontent.app.config.CacheConfig.HOT_FEEDS,
      allEntries = true)
  public void updateTrendingAndSnapshot() {
    if (!enabled) {
      log.debug("[scheduler] 업데이트 스케줄러 OFF");
      return;
    }

    log.info("[scheduler] 시작 (기준: {})", trendingThreshold);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      int snapshot = feedRepository.snapshotViewCount();
      int marked = feedRepository.markTrending(trendingThreshold);
      int unmarked = feedRepository.unmarkTrending(trendingThreshold);


      stopWatch.stop();
      log.info("[scheduler] 완료 - marked: {}, unmarked: {}, snapshot: {}, {}ms",
          marked, unmarked, snapshot, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[scheduler] 실패", e);
    }
  }
}
