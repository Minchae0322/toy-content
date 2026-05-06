package com.example.toycontent.app.battle.repository;

import com.example.toycontent.app.battle.domain.BattleReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleReportRepository extends JpaRepository<BattleReport, Long> {

    boolean existsByBattleIdAndReporterId(Long battleId, Long reporterId);
}
