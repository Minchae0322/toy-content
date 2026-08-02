package com.example.toycontent.app.scheduler;


import com.example.toycontent.app.feed.repository.FeedRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedHotScoreScheduler {

  private static final String DECAY_REDIS_KEY = "feed:hot-score:decay-exponent";
  private static final Duration DECAY_REDIS_TTL = Duration.ofDays(2);

  private final FeedRepository feedRepository;
  private final StringRedisTemplate stringRedisTemplate;

  @Value("${scheduler.feed-hot-score.time-weight-update.enabled}")
  private Boolean timeWeightEnabled;

  @Value("${scheduler.feed-hot-score.full-recalculate.enabled}")
  private Boolean fullRecalculateEnabled;

  @Value("${scheduler.feed-hot-score.full-recalculate.recent-days:30}")
  private int recentDays;

  /**
   * 신규 피드 유입량에 따라 시간 감쇠 지수를 동적으로 선택하기 위한 설정.
   * - window-hours: "최근 신규 피드"의 기준 시간(시간)
   * - threshold: 이 값 이상이면 fast(빠른 감쇠), 미만이면 slow(완만한 감쇠) 적용
   * - fast/slow: POWER(..., exp) 의 지수값. 클수록 시간 감쇠가 강해진다.
   *
   * 결정 시점은 매일의 fullRecalculate. 결정된 값은 Redis에 저장되어
   * 다음 fullRecalculate 전까지 timeWeightUpdate 도 동일한 지수로 재계산한다.
   */
  @Value("${scheduler.feed-hot-score.decay.recent-window-hours:24}")
  private int decayWindowHours;

  @Value("${scheduler.feed-hot-score.decay.threshold:10}")
  private long decayThreshold;

  @Value("${scheduler.feed-hot-score.decay.fast:1.1}")
  private double decayFast;

  @Value("${scheduler.feed-hot-score.decay.slow:0.7}")
  private double decaySlow;

  /**
   * 시간 가중치 재계산 (매시간)
   * - 최근 60분 내 활동이 있는 피드만 벌크 업데이트
   * - 직전 fullRecalculate가 결정한 decay 지수를 그대로 사용
   */
  @Observed(name = "scheduler.feed.hotScore.timeWeight",
      contextualName = "feed-hot-score:time-weight")
  @Scheduled(cron = "${scheduler.feed-hot-score.time-weight-update.cron}")
  @SchedulerLock(
      name = "feedHotScoreTimeWeight",
      lockAtLeastFor = "15m",
      lockAtMostFor = "25m"
  )
  @Transactional
  public void timeWeightUpdate() {
    if (!timeWeightEnabled) {
      log.info("[scheduler] 시간 가중치 업데이트 스케줄러 OFF");
      return;
    }

    log.info("[scheduler] 시간 가중치 업데이트 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      double decayExponent = readActiveDecayExponent();
      long count = feedRepository.bulkUpdateHotScoreRecent(
          LocalDateTime.now().minusMinutes(60), decayExponent);

      stopWatch.stop();
      log.info("[scheduler] 시간 가중치 업데이트 완료 - {}건, decay={}, {}ms",
          count, decayExponent, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[scheduler] 시간 가중치 업데이트 실패", e);
    }
  }

  /**
   * 전체 재계산 (새벽 3시)
   * - 최근 신규 피드 수를 기준으로 decay 지수를 결정하고 Redis에 저장
   * - 최근 N일 피드 전체 벌크 업데이트
   */
  @Observed(name = "scheduler.feed.hotScore.fullRecalculate",
      contextualName = "feed-hot-score:full")
  @Scheduled(cron = "${scheduler.feed-hot-score.full-recalculate.cron}")
  @SchedulerLock(
      name = "feedHotScoreFullRecalculate",
      lockAtLeastFor = "1h",
      lockAtMostFor = "6h"
  )
  @Transactional
  public void fullRecalculate() {
    if (!fullRecalculateEnabled) {
      log.info("[scheduler] 전체 재계산 스케줄러 OFF");
      return;
    }

    log.info("[scheduler] 전체 재계산 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      double decayExponent = decideDecayExponent();
      storeActiveDecayExponent(decayExponent);

      long count = feedRepository.bulkUpdateHotScoreAll(recentDays, decayExponent);

      stopWatch.stop();
      log.info("[scheduler] 전체 재계산 완료 - {}건, decay={}, {}ms",
          count, decayExponent, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[scheduler] 전체 재계산 실패", e);
    }
  }

  /**
   * 최근 신규 피드 수로 decay 지수 결정.
   * 매일 fullRecalculate 시점에만 호출되는 게 정상.
   */
  private double decideDecayExponent() {
    LocalDateTime since = LocalDateTime.now().minusHours(decayWindowHours);
    long recentCount = feedRepository.countRecentFeeds(since);
    boolean fast = recentCount >= decayThreshold;
    double exponent = fast ? decayFast : decaySlow;
    log.info("[scheduler] decay 결정 - 최근 {}시간 신규 {}건, threshold={}, decay={}",
        decayWindowHours, recentCount, decayThreshold, exponent);
    return exponent;
  }

  /**
   * timeWeightUpdate가 사용할 decay 지수 조회.
   * Redis에 저장된 값이 없거나(앱 첫 기동, Redis 일시 장애 등) 읽기 실패 시
   * 즉시 계산해 fallback 하고 동일 값을 저장한다.
   */
  private double readActiveDecayExponent() {
    try {
      String stored = stringRedisTemplate.opsForValue().get(DECAY_REDIS_KEY);
      if (StringUtils.hasText(stored)) {
        return Double.parseDouble(stored);
      }
    } catch (Exception e) {
      log.warn("[scheduler] decay 지수 Redis 조회 실패, fallback 계산", e);
    }
    double fallback = decideDecayExponent();
    storeActiveDecayExponent(fallback);
    return fallback;
  }

  private void storeActiveDecayExponent(double exponent) {
    try {
      stringRedisTemplate.opsForValue()
          .set(DECAY_REDIS_KEY, Double.toString(exponent), DECAY_REDIS_TTL);
    } catch (Exception e) {
      log.warn("[scheduler] decay 지수 Redis 저장 실패", e);
    }
  }
}
