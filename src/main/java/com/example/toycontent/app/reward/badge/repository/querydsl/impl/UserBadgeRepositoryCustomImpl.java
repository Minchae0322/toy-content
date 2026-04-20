package com.example.toycontent.app.reward.badge.repository.querydsl.impl;

import static com.example.toycontent.app.reward.badge.domain.QBadge.badge;
import static com.example.toycontent.app.reward.badge.domain.QUserBadge.userBadge;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.BadgeInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserBadgeInfo;
import com.example.toycontent.app.reward.badge.repository.querydsl.UserBadgeRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserBadgeRepositoryCustomImpl implements UserBadgeRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<UserBadgeInfo> findUserBadgesWithBadgeDetail(Long userId) {
    return queryFactory
        .select(Projections.fields(UserBadgeInfo.class,
            userBadge.id,
            Projections.fields(BadgeInfo.class,
                badge.id,
                badge.code,
                badge.name,
                badge.description,
                badge.iconEmoji,
                badge.iconImageUrl,
                badge.category
            ).as("badge"),
            userBadge.acquiredAt,
            userBadge.pinned
        ))
        .from(userBadge)
        .join(userBadge.badge, badge)
        .where(
            userIdEq(userId),
            userBadge.revoked.isFalse()
        )
        .orderBy(userBadge.acquiredAt.desc())
        .fetch();
  }

  private com.querydsl.core.types.dsl.BooleanExpression userIdEq(Long userId) {
    return userBadge.userId.eq(userId);
  }
}
