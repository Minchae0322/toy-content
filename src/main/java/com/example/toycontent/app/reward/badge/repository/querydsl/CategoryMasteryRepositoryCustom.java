package com.example.toycontent.app.reward.badge.repository.querydsl;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.CategoryMasteryInfo;
import java.util.List;

public interface CategoryMasteryRepositoryCustom {

  List<CategoryMasteryInfo> findMasteriesWithCategoryByUserId(Long userId);

  List<CategoryMasteryInfo> findTopMastersByCategoryId(Long categoryId, int limit);
}
