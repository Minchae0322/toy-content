package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.enumuration.UserTier;
import com.example.toycontent.app.reward.domain.LevelExp;
import com.example.toycontent.app.reward.repository.LevelExpRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelExpService {

  private final LevelExpRepository levelExpRepository;
  private volatile List<LevelExp> levelTable;

  public void reload() {
    this.levelTable = levelExpRepository.findAllByOrderByLevelAsc();
    log.info("레벨 테이블 로드 완료 - {}개 레벨", levelTable.size());
  }

  public LevelInfo computeLevelInfo(long totalExp) {
    List<LevelExp> table = getTable();
    if (table.isEmpty()) {
      return new LevelInfo(1, UserTier.PLAIN, totalExp, 0, false);
    }

    int level = 1;
    long currentLevelExp = totalExp;
    long nextLevelExp = 0;

    for (int i = 1; i < table.size(); i++) {
      LevelExp next = table.get(i);
      if (totalExp >= next.getCumulativeExp()) {
        level = next.getLevel();
        currentLevelExp = totalExp - next.getCumulativeExp();
      } else {
        nextLevelExp = next.getCumulativeExp() - totalExp;
        break;
      }
    }

    boolean maxLevel = level >= getMaxLevel();
    if (maxLevel) {
      currentLevelExp = totalExp - table.get(table.size() - 1).getCumulativeExp();
      nextLevelExp = 0;
    }

    return new LevelInfo(level, UserTier.fromLevel(level), currentLevelExp, nextLevelExp, maxLevel);
  }

  public int getMaxLevel() {
    List<LevelExp> table = getTable();
    if (table.isEmpty()) {
      return 1;
    }
    return table.get(table.size() - 1).getLevel();
  }

  private List<LevelExp> getTable() {
    if (levelTable == null) {
      reload();
    }
    return levelTable;
  }
}
