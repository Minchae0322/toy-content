package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.BattleItemEventEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleItemEventEntryRepository
    extends JpaRepository<BattleItemEventEntry, Long> {
}
