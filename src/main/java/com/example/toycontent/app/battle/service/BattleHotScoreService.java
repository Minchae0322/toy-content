package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.hotscore.domain.HotScoreSettings;
import com.example.toycontent.app.config.CacheConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

/**
 * 배틀 핫 스코어 전체 재계산 (수동 실행 전용).
 *
 * <p>평상시 점수는 투표·참여·스와이프·조회·댓글이 바뀌는 그 배틀에서 바로 갱신된다.
 * {@code hot-score.battle-time-divisor-seconds}를 바꾼 뒤에만 관리자 API에서 부른다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleHotScoreService {

  private final BattleRepository battleRepository;

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.HOT_BATTLES, allEntries = true)
  public int recalculateAll() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    List<Battle> battles = battleRepository.findActiveAndUpcomingBattles();
    battles.forEach(Battle::updateHotScore);
    stopWatch.stop();
    log.info("[hot-score] 배틀 전체 재계산 완료 - {}건, divisor={}s, {}ms",
        battles.size(), HotScoreSettings.battleDivisor(), stopWatch.getTotalTimeMillis());
    return battles.size();
  }
}
