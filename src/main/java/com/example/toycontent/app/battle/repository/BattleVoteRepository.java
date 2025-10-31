package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {

  List<BattleVote> findByBattleItem(BattleItem item);

  List<BattleVote> findByBattleAndUserId(Battle battle, Long userId);

  Boolean existsByBattleIdAndUserId(Long battleId, Long userId);

  int countByBattleAndUserId(Battle battle, Long userId);
}
