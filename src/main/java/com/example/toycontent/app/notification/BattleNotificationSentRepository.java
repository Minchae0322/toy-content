package com.example.toycontent.app.notification;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleNotificationSentRepository extends JpaRepository<BattleNotificationSent, Long> {

  boolean existsByBattleIdAndPhaseAndUserId(Long battleId, BattleNotificationPhase phase, Long userId);

  List<BattleNotificationSent> findByBattleIdInAndPhase(
      Collection<Long> battleIds, BattleNotificationPhase phase);
}
