package com.example.toycontent.app.reward.badge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.CategoryMasteryTier;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.CategoryMasteryInfo;
import com.example.toycontent.app.reward.badge.domain.CategoryMastery;
import com.example.toycontent.app.reward.badge.repository.CategoryMasteryRepository;
import com.example.toycontent.support.fixture.CategoryMasteryFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryMasteryService")
class CategoryMasteryServiceTest {

  private static final Long USER_ID = 100L;
  private static final Long CATEGORY_ID = 10L;

  @Mock private CategoryMasteryRepository categoryMasteryRepository;
  @Mock private CategoryRepository categoryRepository;
  @InjectMocks private CategoryMasteryService categoryMasteryService;

  @Nested
  @DisplayName("incrementFeedCount - 피드 카운트 증가")
  class IncrementFeedCount {

    @Test
    @DisplayName("기존 마스터리의 feedCount를 증가시키고 등급을 재계산한다")
    void 기존_마스터리_증가() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.atTier(CategoryMasteryTier.NONE, 4);
      given(categoryMasteryRepository.findByUserIdAndCategoryId(USER_ID, CATEGORY_ID))
          .willReturn(Optional.of(mastery));

      // when
      CategoryMastery result = categoryMasteryService.incrementFeedCount(USER_ID, CATEGORY_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getFeedCount()).as("피드 카운트").isEqualTo(5);
        softly.assertThat(result.getTier()).as("등급 승급").isEqualTo(CategoryMasteryTier.INTERESTED);
      });
    }

    @Test
    @DisplayName("첫 활동이면 마스터리를 생성 후 증가시킨다")
    void 신규_마스터리_생성() {
      // given
      Category category = Category.builder().id(CATEGORY_ID).name("커피").build();
      given(categoryMasteryRepository.findByUserIdAndCategoryId(USER_ID, CATEGORY_ID))
          .willReturn(Optional.empty());
      given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.of(category));
      given(categoryMasteryRepository.save(any(CategoryMastery.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      CategoryMastery result = categoryMasteryService.incrementFeedCount(USER_ID, CATEGORY_ID);

      // then
      assertThat(result.getFeedCount()).as("피드 카운트").isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("recordPrediction - 예측 기록")
  class RecordPrediction {

    @Test
    @DisplayName("예측 적중 시 적중 수가 증가한다")
    void 적중_기록() {
      // given
      CategoryMastery mastery = CategoryMasteryFixture.withPredictions(10, 5);
      given(categoryMasteryRepository.findByUserIdAndCategoryId(USER_ID, CATEGORY_ID))
          .willReturn(Optional.of(mastery));

      // when
      CategoryMastery result = categoryMasteryService.recordPrediction(
          USER_ID, CATEGORY_ID, true);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getPredictionHitCount()).as("적중 수").isEqualTo(6);
        softly.assertThat(result.getPredictionTotalCount()).as("총 예측").isEqualTo(11);
      });
    }
  }

  @Nested
  @DisplayName("getUserMasteries - 유저 숙련도 목록 조회")
  class GetUserMasteries {

    @Test
    @DisplayName("QueryDSL을 통해 숙련도 목록을 조회한다")
    void 정상_조회() {
      // given
      CategoryMasteryInfo info = CategoryMasteryInfo.builder()
          .categoryId(CATEGORY_ID)
          .categoryName("커피")
          .tier(CategoryMasteryTier.INTERESTED)
          .feedCount(5)
          .build();
      given(categoryMasteryRepository.findMasteriesWithCategoryByUserId(USER_ID))
          .willReturn(List.of(info));

      // when
      List<CategoryMasteryInfo> result = categoryMasteryService.getUserMasteries(USER_ID);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getCategoryName()).isEqualTo("커피");
    }
  }
}
