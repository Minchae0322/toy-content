package com.example.toycontent.app.reward.badge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.toycontent.app.common.enumuration.CategoryMasteryTier;
import com.example.toycontent.support.fixture.CategoryMasteryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CategoryMastery 도메인")
class CategoryMasteryTest {

  @Nested
  @DisplayName("recalculateTier - 등급 재계산")
  class RecalculateTier {

    @Test
    @DisplayName("총 활동 수가 5 미만이면 NONE이다")
    void 등급_NONE() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.fresh();
      for (int i = 0; i < 4; i++) {
        mastery.incrementFeedCount();
      }

      // when
      mastery.recalculateTier();

      // then
      assertThat(mastery.getTier()).isEqualTo(CategoryMasteryTier.NONE);
    }

    @Test
    @DisplayName("총 활동 수가 5 이상이면 INTERESTED이다")
    void 등급_INTERESTED() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.fresh();
      for (int i = 0; i < 5; i++) {
        mastery.incrementFeedCount();
      }

      // when
      mastery.recalculateTier();

      // then
      assertThat(mastery.getTier()).isEqualTo(CategoryMasteryTier.INTERESTED);
    }

    @Test
    @DisplayName("총 활동 수가 100 이상이면 EXPERT이다")
    void 등급_EXPERT() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.atTier(CategoryMasteryTier.CURATOR, 80);
      for (int i = 0; i < 20; i++) {
        mastery.incrementBattleVoteCount();
      }

      // when
      mastery.recalculateTier();

      // then
      assertThat(mastery.getTier()).isEqualTo(CategoryMasteryTier.EXPERT);
    }
  }

  @Nested
  @DisplayName("recordPrediction - 예측 기록")
  class RecordPrediction {

    @Test
    @DisplayName("예측 적중 시 적중 수와 정확도가 올바르게 계산된다")
    void 예측_적중() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.withPredictions(10, 5);

      // when
      mastery.recordPrediction(true);

      // then
      assertSoftly(softly -> {
        softly.assertThat(mastery.getPredictionTotalCount()).as("총 예측").isEqualTo(11);
        softly.assertThat(mastery.getPredictionHitCount()).as("적중 수").isEqualTo(6);
        softly.assertThat(mastery.getPredictionAccuracy())
            .as("정확도")
            .isCloseTo(6.0 / 11.0, org.assertj.core.data.Offset.offset(0.001));
      });
    }

    @Test
    @DisplayName("예측 실패 시 총 수만 증가하고 적중 수는 유지된다")
    void 예측_실패() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.withPredictions(10, 5);

      // when
      mastery.recordPrediction(false);

      // then
      assertSoftly(softly -> {
        softly.assertThat(mastery.getPredictionTotalCount()).as("총 예측").isEqualTo(11);
        softly.assertThat(mastery.getPredictionHitCount()).as("적중 수").isEqualTo(5);
      });
    }
  }
}
