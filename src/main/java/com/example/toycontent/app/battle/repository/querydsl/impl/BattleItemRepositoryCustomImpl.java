package com.example.toycontent.app.battle.repository.querydsl.impl;

import static com.example.toycontent.app.battle.domain.QBattle.battle;
import static com.example.toycontent.app.battle.domain.QBattleItem.battleItem;
import static com.example.toycontent.app.battle.domain.QBattleVote.battleVote;
import static com.example.toycontent.app.product.domain.QProduct.product;

import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.querydsl.BattleItemRepositoryCustom;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.product.service.ProductService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BattleItemRepositoryCustomImpl implements BattleItemRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<BattleItem> findByBattleId(Long battleId, Long currentUserId, BattleItemStatus status) {
    BooleanBuilder whereCondition = whereClause(status);

    JPAQuery<BattleItem> query = queryFactory
        .select(battleItem)
        .from(battleItem)
        .leftJoin(battleItem.product, product).fetchJoin()
        .where(
            battleItem.battle.id.eq(battleId)
                .and(whereCondition)
        )
        .orderBy(
            battleItem.voteCount.desc(),
            new CaseBuilder()
                .when(battleItem.status.eq(BattleItemStatus.ACTIVE)).then(1)
                .when(battleItem.status.eq(BattleItemStatus.UNDER_REVIEW)).then(2)
                .when(battleItem.status.eq(BattleItemStatus.EXCLUDED)).then(3)
                .otherwise(4)
                .asc(),
            battleItem.id.asc()  // 같은 상태 내에서는 ID 순
        );

    return query.fetch();
  }

  /**
   * 배틀 ID 목록에 해당하는 활성 아이템을 점수 내림차순으로 조회
   *
   * displayName, votePercentage 계산을 위해 battle, product fetch join 적용
   * TODO: 추후 배틀 핫 목록을 엔티티로 조회하면 영속성 컨텍스트 활용으로 battle fetch join 제거 가능
   */
  public List<BattleItem> findByBattleIdInAndStatusOrderByTotalScoreDesc(List<Long> battleIds, BattleItemStatus status) {
    return queryFactory
        .selectFrom(battleItem)
        .join(battleItem.battle, battle).fetchJoin()
        .leftJoin(battleItem.product).fetchJoin()
        .where(
            battleItem.battle.id.in(battleIds),
            battleItem.status.eq(status)
        )
        .orderBy(battleItem.totalScore.desc())
        .fetch();
  }



  private BooleanBuilder whereClause(BattleItemStatus status) {
    BooleanBuilder builder = new BooleanBuilder();

    Optional.ofNullable(status)
        .ifPresent(battleItemStatus -> builder.and(battleItem.status.eq(status)));

    return builder;
  }

}
