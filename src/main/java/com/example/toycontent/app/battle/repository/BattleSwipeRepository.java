package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.BattleSwipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleSwipeRepository extends JpaRepository<BattleSwipe, Long> {

  java.util.Optional<BattleSwipe> findByBattle_IdAndBattleItem_IdAndUserId(
      Long battleId, Long itemId, Long userId);

  java.util.Optional<BattleSwipe> findByBattle_IdAndBattleItem_IdAndGuestId(
      Long battleId, Long itemId, String guestId);

  @Query("""
      SELECT s.battleItem.id FROM BattleSwipe s
      WHERE s.battle.id = :battleId AND s.userId = :userId
      """)
  List<Long> findSwipedItemIdsByUser(@Param("battleId") Long battleId,
      @Param("userId") Long userId);

  @Query("""
      SELECT s.battleItem.id FROM BattleSwipe s
      WHERE s.battle.id = :battleId AND s.guestId = :guestId
      """)
  List<Long> findSwipedItemIdsByGuest(@Param("battleId") Long battleId,
      @Param("guestId") String guestId);
}
