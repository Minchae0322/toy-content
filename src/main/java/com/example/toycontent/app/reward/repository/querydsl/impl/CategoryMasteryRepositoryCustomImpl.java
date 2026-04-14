package com.example.toycontent.app.reward.repository.querydsl.impl;

import static com.example.toycontent.app.category.domain.QCategory.category;
import static com.example.toycontent.app.reward.domain.QCategoryMastery.categoryMastery;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.CategoryMasteryInfo;
import com.example.toycontent.app.reward.repository.querydsl.CategoryMasteryRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryMasteryRepositoryCustomImpl implements CategoryMasteryRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<CategoryMasteryInfo> findMasteriesWithCategoryByUserId(Long userId) {
    return queryFactory
        .select(selectMasteryInfo())
        .from(categoryMastery)
        .join(categoryMastery.category, category)
        .where(buildUserWhereClause(userId))
        .orderBy(categoryMastery.tier.desc(), categoryMastery.feedCount.desc())
        .fetch();
  }

  @Override
  public List<CategoryMasteryInfo> findTopMastersByCategoryId(Long categoryId, int limit) {
    return queryFactory
        .select(selectMasteryInfo())
        .from(categoryMastery)
        .join(categoryMastery.category, category)
        .where(buildCategoryWhereClause(categoryId))
        .orderBy(categoryMastery.tier.desc(), categoryMastery.feedCount.desc())
        .limit(limit)
        .fetch();
  }

  private com.querydsl.core.types.QBean<CategoryMasteryInfo> selectMasteryInfo() {
    return Projections.fields(CategoryMasteryInfo.class,
        category.id.as("categoryId"),
        category.name.as("categoryName"),
        categoryMastery.tier,
        categoryMastery.feedCount,
        categoryMastery.battleVoteCount,
        categoryMastery.pickCommentCount,
        categoryMastery.predictionAccuracy
    );
  }

  private BooleanBuilder buildUserWhereClause(Long userId) {
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(categoryMastery.userId.eq(userId));
    return builder;
  }

  private BooleanBuilder buildCategoryWhereClause(Long categoryId) {
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(categoryMastery.category.id.eq(categoryId));
    return builder;
  }
}
