package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.repository.querydsl.BattleItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleItemRepository extends JpaRepository<BattleItem, Long>,
    BattleItemRepositoryCustom {

  long countByBattleAndIsDeletedFalse(Battle battle);
}
