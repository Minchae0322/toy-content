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
    @DisplayName("EXP를 추가하면 totalExp, seasonExp가 모두 증가한다")
    void EXP_추가_정상() {
      // given
      UserReward reward = UserRewardFixture.fresh();

      // when
      reward.addExp(50);

      // then
      assertSoftly(softly -> {
        softly.assertThat(reward.getTotalExp()).as("총 EXP").isEqualTo(50L);
        softly.assertThat(reward.getSeasonExp()).as("시즌 EXP").isEqualTo(50L);
      });
    }

    @Test
    @DisplayName("여러 번 EXP를 추가하면 누적된다")
    void EXP_누적() {
      // given
      UserReward reward = UserRewardFixture.fresh();

      // when
      reward.addExp(100);
      reward.addExp(200);

      // then
      assertThat(reward.getTotalExp()).isEqualTo(300L);
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
