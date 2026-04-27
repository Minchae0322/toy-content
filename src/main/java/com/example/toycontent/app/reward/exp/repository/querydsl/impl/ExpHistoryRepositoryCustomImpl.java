package com.example.toycontent.app.reward.exp.repository.querydsl.impl;

import static com.example.toycontent.app.reward.exp.domain.QExpHistory.expHistory;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.ExpHistoryInfo;
import com.example.toycontent.app.reward.exp.repository.querydsl.ExpHistoryRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExpHistoryRepositoryCustomImpl implements ExpHistoryRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<ExpHistoryInfo> findExpHistoryByUserId(Long userId, Pageable pageable) {
    List<ExpHistoryInfo> content = queryFactory
        .select(Projections.fields(ExpHistoryInfo.class,
            expHistory.id,
            expHistory.userId,
            expHistory.amount,
            expHistory.source,
            expHistory.sourceId,
            expHistory.resultTotalExp,
            expHistory.createdAt
        ))
        .from(expHistory)
        .where(userIdEq(userId))
        .orderBy(expHistory.createdAt.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    return PageableExecutionUtils.getPage(content, pageable, () -> countByUserId(userId));
  }

  private Long countByUserId(Long userId) {
    return queryFactory
        .select(expHistory.count())
        .from(expHistory)
        .where(userIdEq(userId))
        .fetchOne();
  }

  private BooleanExpression userIdEq(Long userId) {
    return expHistory.userId.eq(userId);
  }
}
