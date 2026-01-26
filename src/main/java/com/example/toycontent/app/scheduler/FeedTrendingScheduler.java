package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scheduler.feed-trending", name = "enabled", havingValue = "true")
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
  @Transactional
  public void updateTrendingAndSnapshot() {
    if (!enabled) {
      log.debug("[피드 트렌딩] 업데이트 스케줄러 OFF");
      return;
    }

    log.info("[피드 트렌딩] 시작 (기준: {})", trendingThreshold);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      int marked = feedRepository.markTrending(trendingThreshold);
      int unmarked = feedRepository.unmarkTrending(trendingThreshold);
      int snapshot = feedRepository.snapshotViewCount();

      stopWatch.stop();
      log.info("[피드 트렌딩] 완료 - marked: {}, unmarked: {}, snapshot: {}, {}ms",
          marked, unmarked, snapshot, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[피드 트렌딩] 실패", e);
    }
  }
}
