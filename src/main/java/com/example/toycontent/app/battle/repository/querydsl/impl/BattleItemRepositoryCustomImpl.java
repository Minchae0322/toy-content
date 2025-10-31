package com.example.toycontent.app.battle.repository.querydsl.impl;

import static com.example.toycontent.app.battle.domain.QBattleItem.battleItem;
import static com.example.toycontent.app.battle.domain.QBattleVote.battleVote;

import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.querydsl.BattleItemRepositoryCustom;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BattleItemRepositoryCustomImpl implements BattleItemRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final BattleItemRepository battleItemRepository;

  @Override
  public List<BattleItem> findByBattleIdWithBattleVote(Long battleId, Long currentUserId) {
    JPAQuery<BattleItem> query = queryFactory
        .select(battleItem)
        .from(battleItem)
        .where(battleItem.battle.id.eq(battleId));

    Optional.ofNullable(currentUserId).ifPresent(userId ->
        query.leftJoin(battleItem.battleVotes, battleVote)
            .on(battleVote.userId.eq(userId))
            .fetchJoin()
    );

    return query.fetch();
  }

}
