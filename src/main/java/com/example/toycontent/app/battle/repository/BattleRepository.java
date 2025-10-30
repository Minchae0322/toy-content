package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.querydsl.BattleRepositoryCustom;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleRepository extends JpaRepository<Battle, Long>, BattleRepositoryCustom {
  
  long countByCreatorIdAndStatus(Long userId, BattleStatus battleStatus);

  long countByCreatorIdAndCreatedAtAfter(Long userId, LocalDateTime dayAgo);
}
