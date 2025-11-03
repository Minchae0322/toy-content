package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scheduler.hot-score", name = "enable", havingValue = "true")
public class BattleHotScoreScheduler {

  private final BattleRepository battleRepository;


  /**
   * 시간 가중치 재계산
   * YAML의 scheduler.hot-score.time-weight-update.cron 값 사용
   */
  @Scheduled(cron = "${scheduler.hot-score.time-weight-update.cron}")
  @ConditionalOnProperty(
      prefix = "scheduler.hot-score.time-weight-update",
      name = "enabled",
      havingValue = "true"
  )
  @Transactional
  public void updateTimeWeightedHotScores() {
    long startTime = System.currentTimeMillis();
    log.info("=== 시간 가중치 업데이트 시작 ===");

    List<Battle> battles = battleRepository.findBattlesNeedingTimeWeightUpdate(
        LocalDateTime.now().minusMinutes(30));

    updateBattleHotScores("시간 가중치 업데이트", battles, startTime);
  }

  /**
   * 전체 재계산
   * YAML의 scheduler.hot-score.full-recalculate.cron 값 사용
   */
  @Scheduled(cron = "${scheduler.hot-score.full-recalculate.cron}")
  @ConditionalOnProperty(
      prefix = "scheduler.hot-score.full-recalculate",
      name = "enabled",
      havingValue = "true"
  )
  @Transactional
  public void fullRecalculateHotScores() {
    long startTime = System.currentTimeMillis();
    log.info("=== 전체 재계산 시작 ===");

    List<Battle> battles = battleRepository.findActiveAndUpcomingBattles();

    updateBattleHotScores("전체 재계산", battles, startTime);
  }

  /**
   * 배틀 핫 스코어 업데이트 공통 로직
   */
  private void updateBattleHotScores(String jobName, List<Battle> battles, long startTime) {
    if (battles.isEmpty()) {
      log.info("{}: 업데이트 대상 없음", jobName);
      return;
    }

    log.info("{}: 대상 배틀 수 {}", jobName, battles.size());

    //업데이트
    battles.forEach(Battle::updateHotScore);

    long duration = System.currentTimeMillis() - startTime;

    log.info("=== {} 완료 === 대상: {} 건, 소요시간: {}ms",
        jobName, battles.size(), duration);
  }
}