package com.example.toycontent.app.common;

import com.example.toycontent.app.common.enumuration.ReportReason;
import com.example.toycontent.app.common.enumuration.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    @Comment("신고자 ID")
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    @Comment("신고 사유")
    private ReportReason reason;

    @Column(name = "detail", length = 500)
    @Comment("상세 내용 (자유 입력)")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    @Comment("처리 상태")
    private ReportStatus status;

    protected BaseReport(Long reporterId, ReportReason reason, String detail) {
        this.reporterId = reporterId;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING;
    }

    public void markReviewed() {
        this.status = ReportStatus.REVIEWED;
    }

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
    }
}
