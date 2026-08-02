package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
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
public class BattleHotScoreScheduler {

  private final BattleRepository battleRepository;

  @Value("${scheduler.hot-score.time-weight-update.enabled}")
  private Boolean timeWeightEnabled;

  @Value("${scheduler.hot-score.full-recalculate.enabled}")
  private Boolean fullRecalculateEnabled;

  /**
   * 시간 가중치 재계산 (매시간)
   * - 최근 30분 내 활동이 있는 배틀만 처리
   */
  @Observed(name = "scheduler.battle.hotScore.timeWeight",
      contextualName = "battle-hot-score:time-weight")
  @Scheduled(cron = "${scheduler.hot-score.time-weight-update.cron}")
  @SchedulerLock(
      name = "productPopularity",
      lockAtLeastFor = "15m",
      lockAtMostFor = "25m"
  )
  @Transactional
  public void timeWeightUpdate() {
    if (!timeWeightEnabled) {
      log.debug("[scheduler] 시간 가중치 업데이트 스케줄러 OFF");
      return;
    }

    log.info("[scheduler] 시간 가중치 업데이트 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      List<Battle> battles = battleRepository.findBattlesNeedingTimeWeightUpdate(
          LocalDateTime.now().minusMinutes(30));

      if (battles.isEmpty()) {
        log.info("[scheduler] 시간 가중치 업데이트 완료 - 대상 없음");
        return;
      }

      battles.forEach(Battle::updateHotScore);

      stopWatch.stop();
      log.info("[scheduler] 시간 가중치 업데이트 완료 - {}건, {}ms",
          battles.size(), stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[scheduler] 시간 가중치 업데이트 실패", e);
    }
  }

  /**
   * 전체 재계산 (새벽 3시)
   * - 시간 감쇠 반영을 위해 전체 배틀 재계산
   */
  @Observed(name = "scheduler.battle.hotScore.fullRecalculate",
      contextualName = "battle-hot-score:full")
  @Scheduled(cron = "${scheduler.hot-score.full-recalculate.cron}")
  @SchedulerLock(
      name = "productPopularity",
      lockAtLeastFor = "1h",
      lockAtMostFor = "6h"
  )
  @Transactional
  public void fullRecalculate() {
    if (!fullRecalculateEnabled) {
      log.debug("[scheduler] 전체 재계산 스케줄러 OFF");
      return;
    }

    log.info("[scheduler] 전체 재계산 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      List<Battle> battles = battleRepository.findActiveAndUpcomingBattles();

      if (battles.isEmpty()) {
        log.info("[scheduler] 전체 재계산 완료 - 대상 없음");
        return;
      }

      battles.forEach(Battle::updateHotScore);

      stopWatch.stop();
      log.info("[scheduler] 전체 재계산 완료 - {}건, {}ms",
          battles.size(), stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[scheduler] 전체 재계산 실패", e);
    }
  }
}