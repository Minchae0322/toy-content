package com.example.toycontent.app.battle.repository.querydsl;

import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import java.util.List;

public interface BattleItemRepositoryCustom {

  List<BattleItem> findByBattleIdWithBattleVote(Long battleId, Long currentUserId, BattleItemStatus status);
}
