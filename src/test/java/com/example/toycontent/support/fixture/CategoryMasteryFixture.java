package com.example.toycontent.support.fixture;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.CategoryMasteryTier;
import com.example.toycontent.app.reward.domain.CategoryMastery;

public class CategoryMasteryFixture {

  public static final Long DEFAULT_USER_ID = 100L;
  public static final Long DEFAULT_CATEGORY_ID = 10L;

  private CategoryMasteryFixture() {}

  private static Category defaultCategory() {
    Category parent = Category.builder().id(1L).name("음식").build();
    return Category.builder().id(DEFAULT_CATEGORY_ID).name("커피").parent(parent).build();
  }

  public static CategoryMastery fresh() {
    return CategoryMastery.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .tier(CategoryMasteryTier.NONE)
        .build();
  }

  public static CategoryMastery atTier(CategoryMasteryTier tier, int feedCount) {
    return CategoryMastery.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .tier(tier)
        .feedCount(feedCount)
        .build();
  }

  public static CategoryMastery withPredictions(int total, int hits) {
    double accuracy = total == 0 ? 0.0 : (double) hits / total;
    return CategoryMastery.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .predictionTotalCount(total)
        .predictionHitCount(hits)
        .predictionAccuracy(accuracy)
        .build();
  }
}
