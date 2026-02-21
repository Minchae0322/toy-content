package com.example.toycontent.app.battle.repository.querydsl;

import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import java.util.List;
import java.util.Map;

public interface BattleItemRepositoryCustom {

  List<BattleItem> findByBattleId(Long battleId, Long currentUserId, BattleItemStatus status);

  List<BattleItem> findByBattleIdInAndStatusOrderByTotalScoreDesc(List<Long> battleIds, BattleItemStatus status);

}
