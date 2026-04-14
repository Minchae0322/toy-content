package com.example.toycontent.app.reward.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.toycontent.support.fixture.UserRewardFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserReward 도메인")
class UserRewardTest {

  @Nested
  @DisplayName("addExp - EXP 추가")
  class AddExp {

    @Test
    @DisplayName("EXP를 추가하면 totalExp, currentLevelExp, seasonExp가 모두 증가한다")
    void EXP_추가_정상() {
      // given
      UserReward reward = UserRewardFixture.fresh();

      // when
      reward.addExp(50);

      // then
      assertSoftly(softly -> {
        softly.assertThat(reward.getTotalExp()).as("총 EXP").isEqualTo(50L);
        softly.assertThat(reward.getCurrentLevelExp()).as("현재 레벨 EXP").isEqualTo(50L);
        softly.assertThat(reward.getSeasonExp()).as("시즌 EXP").isEqualTo(50L);
        softly.assertThat(reward.getLevel()).as("레벨 변동 없음").isEqualTo(1);
      });
    }

    @Test
    @DisplayName("EXP가 다음 레벨 기준에 도달하면 자동으로 레벨업한다")
    void 레벨업_발생() {
      // given
      UserReward reward = UserRewardFixture.aboutToLevelUp();

      // when
      reward.addExp(20);

      // then
      assertSoftly(softly -> {
        softly.assertThat(reward.getLevel()).as("레벨").isEqualTo(2);
        softly.assertThat(reward.getCurrentLevelExp()).as("레벨업 후 잔여 EXP").isEqualTo(10L);
        softly.assertThat(reward.getNextLevelExp()).as("다음 레벨 필요 EXP").isEqualTo(200L);
      });
    }

    @Test
    @DisplayName("한 번에 많은 EXP를 얻으면 여러 레벨을 건너뛸 수 있다")
    void 다단_레벨업() {
      // given
      UserReward reward = UserRewardFixture.fresh();

      // when
      reward.addExp(500);

      // then
      assertThat(reward.getLevel()).as("레벨").isGreaterThan(2);
    }
  }

  @Nested
  @DisplayName("addSeasonExp - 시즌 EXP")
  class AddSeasonExp {

    @Test
    @DisplayName("다른 시즌 코드로 추가하면 시즌 EXP가 리셋 후 추가된다")
    void 시즌_변경_시_리셋() {
      // given
      UserReward reward = UserRewardFixture.fresh();
      reward.addSeasonExp(100, "2026-Q1");

      // when
      reward.addSeasonExp(50, "2026-Q2");

      // then
      assertSoftly(softly -> {
        softly.assertThat(reward.getSeasonExp()).as("시즌 EXP").isEqualTo(50L);
        softly.assertThat(reward.getSeasonCode()).as("시즌 코드").isEqualTo("2026-Q2");
      });
    }
  }
}
