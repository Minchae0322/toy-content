package com.example.toycontent.support.fixture;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.reward.domain.BattlePrediction;

public class BattlePredictionFixture {

  public static final Long DEFAULT_USER_ID = 100L;

  private BattlePredictionFixture() {}

  public static BattlePrediction unsettled(Battle battle, BattleItem predictedItem) {
    return BattlePrediction.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .battle(battle)
        .predictedItem(predictedItem)
        .build();
  }
}
