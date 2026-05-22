package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.querydsl.BattleVoteRepositoryCustom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long>,
    BattleVoteRepositoryCustom {

  @Query("""
      SELECT DISTINCT v.userId FROM BattleVote v
      WHERE v.battle.id = :battleId
        AND v.userId IS NOT NULL
      """)
  List<Long> findDistinctVoterUserIdsByBattleId(@Param("battleId") Long battleId);

  List<BattleVote> findByBattleItem(BattleItem item);

  List<BattleVote> findByBattleAndUserId(Battle battle, Long userId);

  Boolean existsByBattleIdAndUserId(Long battleId, Long userId);

  int countByBattleAndUserId(Battle battle, Long userId);

  List<BattleVote> findByBattle_IdAndUserId(Long battleId, Long userId);

  List<BattleVote> findByBattleAndGuestId(Battle battle, String guestId);

  List<BattleVote> findByBattle_IdAndGuestId(Long battleId, String guestId);
}
