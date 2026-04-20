package com.example.toycontent.support.fixture;

import com.example.toycontent.app.reward.exp.domain.UserReward;

public class UserRewardFixture {

  public static final Long DEFAULT_USER_ID = 100L;

  private UserRewardFixture() {}

  public static UserReward fresh() {
    return UserReward.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .totalExp(0L)
        .seasonExp(0L)
        .build();
  }

  public static UserReward withTotalExp(long totalExp) {
    return UserReward.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .totalExp(totalExp)
        .seasonExp(0L)
        .build();
  }
}
