package com.example.toycontent.app.scheduler;


import com.example.toycontent.app.feed.repository.FeedRepository;
import java.time.LocalDateTime;
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
@ConditionalOnProperty(prefix = "scheduler.feed-hot-score", name = "enabled", havingValue = "true")
public class FeedHotScoreScheduler {

  private final FeedRepository feedRepository;

  @Value("${scheduler.feed-hot-score.time-weight-update.enabled}")
  private Boolean timeWeightEnabled;

  @Value("${scheduler.feed-hot-score.full-recalculate.enabled}")
  private Boolean fullRecalculateEnabled;

  @Value("${scheduler.feed-hot-score.full-recalculate.recent-days:30}")
  private int recentDays;

  /**
   * 시간 가중치 재계산 (매시간)
   * - 최근 30분 내 활동이 있는 피드만 벌크 업데이트
   */
  @Scheduled(cron = "${scheduler.feed-hot-score.time-weight-update.cron}")
  @SchedulerLock(
      name = "feedHotScoreTimeWeight",
      lockAtLeastFor = "15m",
      lockAtMostFor = "25m"
  )
  @Transactional
  public void timeWeightUpdate() {
    if (!timeWeightEnabled) {
      log.info("[피드 핫 스코어] 시간 가중치 업데이트 스케줄러 OFF");
      return;
    }

    log.info("[피드 핫 스코어] 시간 가중치 업데이트 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      long count = feedRepository.bulkUpdateHotScoreRecent(
          LocalDateTime.now().minusMinutes(30));

      stopWatch.stop();
      log.info("[피드 핫 스코어] 시간 가중치 업데이트 완료 - {}건, {}ms",
          count, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[피드 핫 스코어] 시간 가중치 업데이트 실패", e);
    }
  }

  /**
   * 전체 재계산 (새벽 3시)
   * - 시간 감쇠 반영을 위해 최근 N일 피드 전체 벌크 업데이트
   */
  @Scheduled(cron = "${scheduler.feed-hot-score.full-recalculate.cron}")
  @SchedulerLock(
      name = "feedHotScoreFullRecalculate",
      lockAtLeastFor = "1h",
      lockAtMostFor = "6h"
  )
  @Transactional
  public void fullRecalculate() {
    if (!fullRecalculateEnabled) {
      log.info("[피드 핫 스코어] 전체 재계산 스케줄러 OFF");
      return;
    }

    log.info("[피드 핫 스코어] 전체 재계산 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      long count = feedRepository.bulkUpdateHotScoreAll(recentDays);

      stopWatch.stop();
      log.info("[피드 핫 스코어] 전체 재계산 완료 - {}건, {}ms",
          count, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[피드 핫 스코어] 전체 재계산 실패", e);
    }
  }
}
