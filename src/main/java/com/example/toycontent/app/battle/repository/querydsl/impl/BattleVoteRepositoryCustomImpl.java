package com.example.toycontent.app.battle.repository.querydsl.impl;

import static com.example.toycontent.app.battle.domain.QBattleVote.battleVote;

import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.querydsl.BattleVoteRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
public class BattleVoteRepositoryCustomImpl implements BattleVoteRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  /**
   * 특정 사용자의 투표 정보를 배틀 아이템 ID 기준으로 일괄 조회
   * <p>
   * 배틀 아이템 컬렉션(battleVotes)의 lazy loading으로 전체 투표가 로드되는 문제를 방지하기 위해,
   * 현재 사용자의 투표만 별도 쿼리로 조회한다.
   *
   * @param battleItemIds 조회 대상 배틀 아이템 ID 목록
   * @param userId        현재 사용자 ID (null이면 빈 맵 반환)
   * @return 배틀 아이템 ID → 해당 사용자의 투표 매핑 (투표하지 않은 아이템은 맵에 미포함)
   */
  @Override
  public Map<Long, BattleVote> findUserVotesByBattleItemIds(List<Long> battleItemIds, Long userId) {
    if (userId == null || battleItemIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return queryFactory
        .selectFrom(battleVote)
        .where(
            battleVote.battleItem.id.in(battleItemIds),
            battleVote.userId.eq(userId)
        )
        .fetch()
        .stream()
        .collect(Collectors.toMap(
            vote -> vote.getBattleItem().getId(),
            Function.identity()
        ));
  }

  @Override
  public List<Long> findDistinctVoterUserIdsByBattleId(Long battleId) {
    return queryFactory
        .select(battleVote.userId).distinct()
        .from(battleVote)
        .where(
            battleVote.battle.id.eq(battleId),
            battleVote.userId.isNotNull()
        )
        .fetch();
  }
}
