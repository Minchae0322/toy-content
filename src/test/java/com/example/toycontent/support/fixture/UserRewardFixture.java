package com.example.toycontent.support.fixture;

import com.example.toycontent.app.reward.domain.UserReward;

public class UserRewardFixture {

  public static final Long DEFAULT_USER_ID = 100L;

  private UserRewardFixture() {}

  public static UserReward fresh() {
    return UserReward.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .totalExp(0L)
        .level(1)
        .currentLevelExp(0L)
        .nextLevelExp(100L)
        .seasonExp(0L)
        .build();
  }

  public static UserReward atLevel(int level) {
    long nextLevelExp = 100L * level;
    return UserReward.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .totalExp(0L)
        .level(level)
        .currentLevelExp(0L)
        .nextLevelExp(nextLevelExp)
        .seasonExp(0L)
        .build();
  }

  public static UserReward aboutToLevelUp() {
    return UserReward.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .totalExp(90L)
        .level(1)
        .currentLevelExp(90L)
        .nextLevelExp(100L)
        .seasonExp(90L)
        .build();
  }
}
