package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.controller.dto.BattleReportRequest;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleReport;
import com.example.toycontent.app.battle.repository.BattleReportRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BattleReportService {

    private final BattleRepository battleRepository;
    private final BattleReportRepository battleReportRepository;

    public Long report(Long battleId, Long reporterId, BattleReportRequest request) {
        Battle battle = battleRepository.findById(battleId)
            .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_NOT_FOUND));

        if (battle.getCreatorId().equals(reporterId)) {
            throw new RestApiException(BattleErrorCode.BATTLE_REPORT_SELF);
        }

        if (battleReportRepository.existsByBattleIdAndReporterId(battleId, reporterId)) {
            throw new RestApiException(BattleErrorCode.BATTLE_REPORT_DUPLICATED);
        }

        BattleReport report = BattleReport.of(battle, reporterId, request.getReason(), request.getDetail());
        return battleReportRepository.save(report).getId();
    }
}
