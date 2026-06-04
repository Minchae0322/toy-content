package com.example.toycontent.app.battle.controller.dto;

import static com.example.toycontent.app.common.utils.BattleItemRankingCalculator.setRanking;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleItemInfo;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BattleItemInfo - voteType 분기")
class BattleItemInfoTest {

  @Test
  @DisplayName("SWIPE 배틀: swipeStats가 채워지고 getRankingScore가 swipe 점수를 반환한다")
  void SWIPE_배틀_swipeStats_채워짐() {
    // given
    Battle battle = swipeBattle();
    BattleItem item = swipeItem(battle, 10L, 2, 3, 1); // score = 2*3+3 = 9

    // when
    BattleItemInfo info = BattleItemInfo.from(item, null, null);

    // then
    assertThat(info.getSwipeStats()).isNotNull();
    assertThat(info.getSwipeStats().getStrongPickCount()).isEqualTo(2);
    assertThat(info.getSwipeStats().getPickCount()).isEqualTo(3);
    assertThat(info.getSwipeStats().getPassCount()).isEqualTo(1);
    assertThat(info.getSwipeStats().getScore()).isEqualTo(9);
    assertThat(info.getRankingScore()).isEqualTo(9);
  }

  @Test
  @DisplayName("vote 배틀: swipeStats가 null이고 getRankingScore는 totalScore를 반환한다")
  void VOTE_배틀_swipeStats_null() {
    // given
    Battle battle = voteBattle();
    BattleItem item = voteItem(battle, 10L, 5);

    // when
    BattleItemInfo info = BattleItemInfo.from(item, null, null);

    // then
    assertThat(info.getSwipeStats()).isNull();
    assertThat(info.getRankingScore()).isEqualTo(5);
  }

  @Test
  @DisplayName("setRanking: SWIPE 배틀이면 swipe 점수 기준으로 rank가 부여된다")
  void setRanking_SWIPE_배틀_swipe점수_기준() {
    // given - vote totalScore는 일부러 거꾸로(=10/5/15)이지만 swipe 점수는 9/3/1
    Battle battle = swipeBattle();
    BattleItem a = swipeItem(battle, 10L, 2, 3, 0); // score 9
    BattleItem b = swipeItem(battle, 20L, 1, 1, 0); // score 4
    BattleItem c = swipeItem(battle, 30L, 0, 1, 0); // score 1

    List<BattleItemInfo> items = List.of(
        BattleItemInfo.from(a, null, null),
        BattleItemInfo.from(b, null, null),
        BattleItemInfo.from(c, null, null));

    // when
    setRanking(items);

    // then - rank는 swipe score 기준
    assertThat(items.get(0).getRank()).isEqualTo(1); // a
    assertThat(items.get(1).getRank()).isEqualTo(2); // b
    assertThat(items.get(2).getRank()).isEqualTo(3); // c
  }

  // ==================== helpers ====================

  private Battle swipeBattle() {
    LocalDateTime now = LocalDateTime.now();
    return Battle.builder()
        .id(1L)
        .title("스와이프")
        .creatorId(100L)
        .startDate(now.minusDays(1))
        .participationStartDate(now.minusDays(1))
        .endDate(now.plusDays(7))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.SWIPE)
        .status(BattleStatus.NORMAL)
        .build();
  }

  private Battle voteBattle() {
    LocalDateTime now = LocalDateTime.now();
    return Battle.builder()
        .id(2L)
        .title("일반")
        .creatorId(100L)
        .startDate(now.minusDays(1))
        .participationStartDate(now.minusDays(1))
        .endDate(now.plusDays(7))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.MULTIPLE)
        .status(BattleStatus.NORMAL)
        .build();
  }

  private BattleItem swipeItem(Battle battle, Long id, int strong, int pick, int pass) {
    return BattleItem.builder()
        .id(id)
        .battle(battle)
        .itemType(BattleItemType.CUSTOM)
        .customName("아이템 " + id)
        .registerId(100L)
        .status(BattleItemStatus.ACTIVE)
        .strongPickCount(strong)
        .pickCount(pick)
        .passCount(pass)
        .build();
  }

  private BattleItem voteItem(Battle battle, Long id, int totalScore) {
    return BattleItem.builder()
        .id(id)
        .battle(battle)
        .itemType(BattleItemType.CUSTOM)
        .customName("아이템 " + id)
        .registerId(100L)
        .status(BattleItemStatus.ACTIVE)
        .totalScore(totalScore)
        .build();
  }
}
