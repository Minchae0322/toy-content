package com.example.toycontent.app.battle.repository.querydsl;

import com.example.toycontent.app.battle.domain.BattleVote;
import java.util.List;
import java.util.Map;

public interface BattleVoteRepositoryCustom {

  Map<Long, BattleVote> findUserVotesByBattleItemIds(List<Long> battleItemIds, Long userId);

  List<Long> findDistinctVoterUserIdsByBattleId(Long battleId);

}
