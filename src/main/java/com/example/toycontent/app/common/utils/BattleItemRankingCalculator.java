package com.example.toycontent.app.common.utils;

import com.example.toycontent.app.battle.controller.dto.Rankable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BattleItemRankingCalculator {

  public static <T extends Rankable> Map<Long, Integer> getRanking(List<T> items) {
    List<T> sorted = items.stream()
        .sorted()
        .toList();

    Map<Long, Integer> ranking = new HashMap<>();
    int rank = 0;
    Integer prevScore = null;

    for (int i = 0; i < sorted.size(); i++) {
      Rankable item = sorted.get(i);
      if (!item.getTotalScore().equals(prevScore)) {
        rank = i + 1;
        prevScore = item.getTotalScore();
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
