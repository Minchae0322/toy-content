package com.example.toycontent.app.notification.hotcontent;

import static com.example.toycontent.app.battle.domain.QBattle.battle;

import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BattleHotContentSource implements HotContentSource {

  private final JPAQueryFactory queryFactory;

  @Override
  public HotContentType type() {
    return HotContentType.BATTLE;
  }

  @Override
  public List<HotContentCandidate> findTopCandidates(int limit) {
    List<Tuple> rows = queryFactory
        .select(battle.id, battle.title, battle.hotScore)
        .from(battle)
        .where(
            battle.isDeleted.isFalse(),
            battle.status.eq(BattleStatus.NORMAL),
            battle.endDate.gt(LocalDateTime.now()),
            battle.hotScore.gt(0.0)
        )
        .orderBy(battle.hotScore.desc(), battle.id.desc())
        .limit(limit)
        .fetch();

    return rows.stream()
        .map(row -> HotContentCandidate.builder()
            .type(HotContentType.BATTLE)
            .contentId(row.get(battle.id))
            .displayName(row.get(battle.title))
            .hotScore(row.get(battle.hotScore))
            .build())
        .toList();
  }
}
