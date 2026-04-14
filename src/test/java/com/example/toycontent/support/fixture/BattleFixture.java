package com.example.toycontent.support.fixture;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import java.time.LocalDateTime;

public class BattleFixture {

  public static final Long DEFAULT_BATTLE_ID = 1L;
  public static final Long DEFAULT_CREATOR_ID = 100L;

  private BattleFixture() {}

  private static Category defaultCategory() {
    Category parent = Category.builder().id(1L).name("음식").build();
    return Category.builder().id(20L).name("커피").parent(parent).build();
  }

  public static Battle active() {
    LocalDateTime now = LocalDateTime.now();
    return Battle.builder()
        .id(DEFAULT_BATTLE_ID)
        .title("테스트 배틀")
        .description("테스트 배틀 설명")
        .category(defaultCategory())
        .creatorId(DEFAULT_CREATOR_ID)
        .startDate(now.minusDays(1))
        .participationStartDate(now.minusDays(1))
        .endDate(now.plusDays(7))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.MULTIPLE)
        .status(BattleStatus.NORMAL)
        .build();
  }

  public static Battle suspended() {
    LocalDateTime now = LocalDateTime.now();
    return Battle.builder()
        .id(DEFAULT_BATTLE_ID)
        .title("정지된 배틀")
        .category(defaultCategory())
        .creatorId(DEFAULT_CREATOR_ID)
        .startDate(now.minusDays(10))
        .participationStartDate(now.minusDays(10))
        .endDate(now.minusDays(1))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.MULTIPLE)
        .status(BattleStatus.SUSPENDED)
        .build();
  }
}
