package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleHotScoreScheduler {

  private final BattleRepository battleRepository;


  /**
   * 30분마다 시간 가중치 재계산
   */
  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void updateTimeWeightedHotScores() {
    long startTime = System.currentTimeMillis();
    log.info("=== 시간 가중치 업데이트 시작 ===");

    List<Battle> battles = battleRepository.findBattlesNeedingTimeWeightUpdate(
        LocalDateTime.now().minusMinutes(30));

    updateBattleHotScores("시간 가중치 업데이트", battles, startTime);
  }

  /**
   * 매일 새벽 3시에 전체 재계산
   */
  @Scheduled(cron = "0 0 3 * * *")
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