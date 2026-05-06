package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseReport;
import com.example.toycontent.app.common.enumuration.ReportReason;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "tb_battle_report",
    indexes = {
        @Index(name = "idx_battle_report_battle", columnList = "battle_id"),
        @Index(name = "idx_battle_report_status", columnList = "status, created_at DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_battle_report_battle_reporter", columnNames = {"battle_id", "reporter_id"})
    }
)
public class BattleReport extends BaseReport {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    private BattleReport(Battle battle, Long reporterId, ReportReason reason, String detail) {
        super(reporterId, reason, detail);
        this.battle = battle;
    }

    public static BattleReport of(Battle battle, Long reporterId, ReportReason reason, String detail) {
        return new BattleReport(battle, reporterId, reason, detail);
    }
}
