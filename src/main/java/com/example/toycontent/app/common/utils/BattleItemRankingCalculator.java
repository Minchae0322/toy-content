package com.example.toycontent.app.common.utils;

import com.example.toycontent.app.battle.controller.dto.Rankable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BattleItemRankingCalculator {

  /**
   * 아이템 리스트에 동순위(1224 스타일) 랭킹을 계산한다.
   *
   * <p>동점은 같은 순위, 다음 순위는 동점자 수만큼 건너뛴다.
   * <pre>
   *   점수 [10, 10, 8, 5] → 순위 [1, 1, 3, 4]
   * </pre>
   *
   * <p>정렬은 {@link Rankable#compareTo}가 {@code rankingScore} 내림차순으로 정의한다.
   * voteType별 점수 모델(예: SWIPE는 strong*3 + pick*1)은 {@link Rankable#getRankingScore}에 캡슐화되어 있어
   * 본 메서드는 voteType 분기 없이 동작한다.
   *
   * @return {@code itemId → rank} 매핑. 입력 리스트 순서는 보존하며 rank만 별도 맵으로 돌려준다.
   */
  public static <T extends Rankable> Map<Long, Integer> getRanking(List<T> items) {
    List<T> sorted = items.stream()
        .sorted()
        .toList();

    Map<Long, Integer> ranking = new HashMap<>();
    int rank = 0;
    Integer prevScore = null;

    for (int i = 0; i < sorted.size(); i++) {
      Rankable item = sorted.get(i);
      // 동점이면 직전 rank 유지, 점수가 떨어지는 시점에만 i+1로 점프 → 자연스럽게 동점자 수만큼 건너뜀
      if (!item.getRankingScore().equals(prevScore)) {
        rank = i + 1;
        prevScore = item.getRankingScore();
      }
      ranking.put(item.getId(), rank);
    }

    return ranking;
  }

  public static <T extends Rankable> List<T> setRanking(List<T> items) {
    Map<Long, Integer> rankMap = getRanking(items);
    items.forEach(item -> item.setRank(rankMap.get(item.getId())));
    return items;
  }
}
