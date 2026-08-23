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

  // 레벨 테이블은 부팅 시 LevelExpDataInitializer가 넣는 정적 데이터인데, 종전에는
  // 피드 목록·상세 요청마다 DB에서 다시 읽었다 (극한 부하에서 초당 200회+).
  // 인메모리 TTL 캐시로 전환 - 운영 중 테이블을 손대는 경로가 없어 TTL은 안전망이다.
  private static final long TABLE_CACHE_TTL_MILLIS = 10 * 60 * 1000L;
  private volatile List<LevelExp> cachedLevelTable;
  private volatile long cachedLevelTableAt;

  public LevelInfo computeLevelInfo(long totalExp) {
    return computeLevelInfo(totalExp, getLevelTable());
  }

  public LevelInfo computeLevelInfo(long totalExp, List<LevelExp> levelTable) {
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

  public List<LevelExp> getLevelTable() {
    List<LevelExp> table = cachedLevelTable;
    if (table != null && System.currentTimeMillis() - cachedLevelTableAt < TABLE_CACHE_TTL_MILLIS) {
      return table;
    }
    table = List.copyOf(levelExpRepository.findAllByOrderByLevelAsc());
    cachedLevelTable = table;
    cachedLevelTableAt = System.currentTimeMillis();
    return table;
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
