package com.example.toycontent.app.reward.badge.service;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.CategoryMasteryInfo;
import com.example.toycontent.app.reward.badge.domain.CategoryMastery;
import com.example.toycontent.app.reward.badge.repository.CategoryMasteryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryMasteryService {

  private final CategoryMasteryRepository categoryMasteryRepository;
  private final CategoryRepository categoryRepository;

  @Transactional
  public CategoryMastery incrementFeedCount(Long userId, Long categoryId) {
    CategoryMastery mastery = getOrCreate(userId, categoryId);
    mastery.incrementFeedCount();
    mastery.recalculateTier();
    return mastery;
  }

  @Transactional
  public CategoryMastery incrementBattleVoteCount(Long userId, Long categoryId) {
    CategoryMastery mastery = getOrCreate(userId, categoryId);
    mastery.incrementBattleVoteCount();
    mastery.recalculateTier();
    return mastery;
  }

  @Transactional
  public CategoryMastery incrementPickCommentCount(Long userId, Long categoryId) {
    CategoryMastery mastery = getOrCreate(userId, categoryId);
    mastery.incrementPickCommentCount();
    mastery.recalculateTier();
    return mastery;
  }

  @Transactional
  public CategoryMastery recordPrediction(Long userId, Long categoryId, boolean hit) {
    CategoryMastery mastery = getOrCreate(userId, categoryId);
    mastery.recordPrediction(hit);
    return mastery;
  }

  public CategoryMastery getUserCategoryMastery(Long userId, Long categoryId) {
    return categoryMasteryRepository.findByUserIdAndCategoryId(userId, categoryId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.CATEGORY_MASTERY_NOT_FOUND));
  }

  public List<CategoryMasteryInfo> getUserMasteries(Long userId) {
    return categoryMasteryRepository.findMasteriesWithCategoryByUserId(userId);
  }

  public List<CategoryMasteryInfo> getTopMasters(Long categoryId, int limit) {
    return categoryMasteryRepository.findTopMastersByCategoryId(categoryId, limit);
  }

  private CategoryMastery getOrCreate(Long userId, Long categoryId) {
    return categoryMasteryRepository.findByUserIdAndCategoryId(userId, categoryId)
        .orElseGet(() -> {
          Category category = categoryRepository.findById(categoryId)
              .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
          return categoryMasteryRepository.save(createMastery(userId, category));
        });
  }

  private CategoryMastery createMastery(Long userId, Category category) {
    return CategoryMastery.builder()
        .userId(userId)
        .category(category)
        .build();
  }
}
