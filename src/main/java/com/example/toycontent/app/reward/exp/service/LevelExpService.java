package com.example.toycontent.app.reward.exp.service;

import com.example.toycontent.app.common.enumuration.UserTier;
import com.example.toycontent.app.reward.exp.domain.LevelExp;
import com.example.toycontent.app.reward.exp.repository.LevelExpRepository;
import com.example.toycontent.app.reward.exp.service.dto.LevelInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LevelExpService {

  private final LevelExpRepository levelExpRepository;

  public LevelInfo computeLevelInfo(long totalExp) {
    List<LevelExp> levelTable = levelExpRepository.findAllByOrderByLevelAsc();

    if (levelTable.isEmpty()) {
      return new LevelInfo(1, UserTier.PLAIN, totalExp, 0, false);
    }

    LevelExp current = findCurrentLevel(levelTable, totalExp);
    LevelExp next = findNextLevel(levelTable, totalExp);
    boolean isMaxLevel = next == null;

    long currentLevelExp = totalExp - current.getCumulativeExp();
    long nextLevelExp = isMaxLevel ? 0 : next.getCumulativeExp() - totalExp;

    return new LevelInfo(
        current.getLevel(),
        UserTier.fromLevel(current.getLevel()),
        currentLevelExp,
        nextLevelExp,
        isMaxLevel);
  }

  /**
   * totalExp 이하의 cumulativeExp를 가진 가장 높은 레벨을 반환한다.
   */
  private LevelExp findCurrentLevel(List<LevelExp> table, long totalExp) {
    for (int i = table.size() - 1; i >= 0; i--) {
      if (table.get(i).getCumulativeExp() <= totalExp) {
        return table.get(i);
      }
    }
    return table.get(0);
  }

  /**
   * totalExp 초과의 cumulativeExp를 가진 가장 낮은 레벨(다음 레벨)을 반환한다.
   * 최대 레벨이면 null을 반환한다.
   */
  private LevelExp findNextLevel(List<LevelExp> table, long totalExp) {
    for (LevelExp entry : table) {
      if (entry.getCumulativeExp() > totalExp) {
        return entry;
      }
    }
    return null;
  }
}
