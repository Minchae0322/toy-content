package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleItemRepository extends JpaRepository<BattleItem, Long> {

  long countByBattleAndIsDeletedFalse(Battle battle);
}
