package com.example.toycontent.app.battle.repository.querydsl.impl;

import static com.example.toycontent.app.battle.domain.QBattleItemComment.battleItemComment;
import static com.example.toycontent.app.battle.domain.QBattleItemCommentLike.battleItemCommentLike;

import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.Detail;
import com.example.toycontent.app.battle.repository.querydsl.BattleItemCommentRepositoryCustom;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class BattleItemCommentRepositoryImpl implements BattleItemCommentRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  @Override
  public Slice<Detail> findBattleItemComments(Long itemId, Long userId, Pageable pageable) {

    List<Detail> content = queryFactory
        .select(Projections.fields(BattleItemCommentResponse.Detail.class,
            battleItemComment.id.as("commentId"),
            battleItemComment.battleItem.id.as("battleItemId"),
            battleItemComment.creatorId,
            battleItemComment.creatorNickname,
            battleItemComment.creatorProfileImageUrl,
            battleItemComment.content,
            battleItemComment.likeCount,
            isLikedExpression(userId),
            isMineExpression(userId),
            battleItemComment.createdAt
        ))
        .from(battleItemComment)
        .leftJoin(battleItemCommentLike)
        .on(battleItemCommentLike.battleItemComment.id.eq(battleItemComment.id),
            likeJoinCondition(userId))
        .where(
            battleItemComment.battleItem.id.eq(itemId),
            battleItemComment.isDeleted.isFalse()
        )
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize() + 1)
        .fetch();

    boolean hasNext = content.size() > pageable.getPageSize();
    if (hasNext) {
      content.remove(content.size() - 1);
    }

    return new SliceImpl<>(content, pageable, hasNext);
  }

  @Override
  public Slice<Detail> findBattleComments(Long battleId, Long userId, Pageable pageable) {

    List<Detail> content = queryFactory
        .select(Projections.fields(BattleItemCommentResponse.Detail.class,
            battleItemComment.id.as("commentId"),
            battleItemComment.battleItem.id.as("battleItemId"),
            battleItemComment.creatorId,
            battleItemComment.creatorNickname,
            battleItemComment.creatorProfileImageUrl,
            battleItemComment.content,
            battleItemComment.likeCount,
            isLikedExpression(userId),
            isMineExpression(userId),
            battleItemComment.createdAt
        ))
        .from(battleItemComment)
        .leftJoin(battleItemCommentLike)
        .on(battleItemCommentLike.battleItemComment.id.eq(battleItemComment.id),
            likeJoinCondition(userId))
        .where(
            battleItemComment.battleItem.battle.id.eq(battleId),
            battleItemComment.isDeleted.isFalse()
        )
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize() + 1)
        .fetch();

    boolean hasNext = content.size() > pageable.getPageSize();
    if (hasNext) {
      content.remove(content.size() - 1);
    }

    return new SliceImpl<>(content, pageable, hasNext);
  }


  private BooleanExpression likeJoinCondition(Long userId) {
    return userId != null
        ? battleItemCommentLike.creatorId.eq(userId)
        : battleItemCommentLike.id.isNull();
  }

  private SimpleExpression<Boolean> isLikedExpression(Long userId) {
    return userId != null
        ? battleItemCommentLike.id.isNotNull().as("isLiked")
        : Expressions.asBoolean(Expressions.constant(false)).as("isLiked");
  }

  private SimpleExpression<Boolean> isMineExpression(Long userId) {
    return userId != null
        ? battleItemComment.creatorId.eq(userId).as("isMine")
        : Expressions.asBoolean(Expressions.constant(false)).as("isMine");
  }

  private OrderSpecifier<?> getOrderSpecifier(Sort sort) {
    for (Sort.Order order : sort) {
      switch (order.getProperty()) {
        case "createdAt":
          return battleItemComment.createdAt.desc();
        case "likeCount":
        default:
          return battleItemComment.likeCount.desc();
      }
    }
    return battleItemComment.likeCount.desc();
  }
}
