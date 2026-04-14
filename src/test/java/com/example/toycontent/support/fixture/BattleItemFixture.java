package com.example.toycontent.support.fixture;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;

public class BattleItemFixture {

  public static final Long DEFAULT_ITEM_ID = 10L;
  public static final Long DEFAULT_REGISTERER_ID = 300L;

  private BattleItemFixture() {}

  public static BattleItem custom(Battle battle, String name) {
    return BattleItem.builder()
        .id(DEFAULT_ITEM_ID)
        .battle(battle)
        .itemType(BattleItemType.CUSTOM)
        .customName(name)
        .customImageUrl("https://example.com/custom.png")
        .registerId(DEFAULT_REGISTERER_ID)
        .status(BattleItemStatus.ACTIVE)
        .build();
  }

  public static BattleItem youtube(Battle battle, String videoId) {
    return BattleItem.builder()
        .id(DEFAULT_ITEM_ID)
        .battle(battle)
        .itemType(BattleItemType.YOUTUBE)
        .customName("유튜브 아이템")
        .contentUrl("https://www.youtube.com/watch?v=" + videoId)
        .contentId(videoId)
        .registerId(DEFAULT_REGISTERER_ID)
        .status(BattleItemStatus.ACTIVE)
        .build();
  }

  public static BattleItem withStatus(Battle battle, BattleItemStatus status) {
    return BattleItem.builder()
        .id(DEFAULT_ITEM_ID)
        .battle(battle)
        .itemType(BattleItemType.CUSTOM)
        .customName("테스트 아이템")
        .registerId(DEFAULT_REGISTERER_ID)
        .status(status)
        .build();
  }

  public static BattleItem deleted(Battle battle) {
    return BattleItem.builder()
        .id(DEFAULT_ITEM_ID)
        .battle(battle)
        .itemType(BattleItemType.CUSTOM)
        .customName("삭제된 아이템")
        .registerId(DEFAULT_REGISTERER_ID)
        .status(BattleItemStatus.ACTIVE)
        .isDeleted(true)
        .build();
  }
}
