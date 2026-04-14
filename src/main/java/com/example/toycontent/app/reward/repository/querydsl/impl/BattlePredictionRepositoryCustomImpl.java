package com.example.toycontent.app.reward.repository.querydsl.impl;

import static com.example.toycontent.app.reward.domain.QBattlePrediction.battlePrediction;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.PredictionInfo;
import com.example.toycontent.app.reward.repository.querydsl.BattlePredictionRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BattlePredictionRepositoryCustomImpl implements BattlePredictionRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<PredictionInfo> findPredictionHistoryByUserIdWithDetail(Long userId,
      Pageable pageable) {
    return queryFactory
        .select(Projections.fields(PredictionInfo.class,
            battlePrediction.id,
            battlePrediction.battle.id.as("battleId"),
            battlePrediction.predictedItem.id.as("predictedItemId"),
            battlePrediction.winnerItem.id.as("winnerItemId"),
            battlePrediction.hit,
            battlePrediction.settledAt,
            battlePrediction.createdAt
        ))
        .from(battlePrediction)
        .where(buildWhereClause(userId))
        .orderBy(battlePrediction.createdAt.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  @Override
  public Long countPredictionHistory(Long userId) {
    return queryFactory
        .select(battlePrediction.count())
        .from(battlePrediction)
        .where(buildWhereClause(userId))
        .fetchOne();
  }

  private BooleanBuilder buildWhereClause(Long userId) {
    BooleanBuilder builder = new BooleanBuilder();
    builder.and(battlePrediction.userId.eq(userId));
    return builder;
  }
}
