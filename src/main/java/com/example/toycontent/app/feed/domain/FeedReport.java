package com.example.toycontent.app.feed.domain;

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
    name = "tb_feed_report",
    indexes = {
        @Index(name = "idx_feed_report_feed", columnList = "feed_id"),
        @Index(name = "idx_feed_report_status", columnList = "status, created_at DESC")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_feed_report_feed_reporter", columnNames = {"feed_id", "reporter_id"})
    }
)
public class FeedReport extends BaseReport {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    private FeedReport(Feed feed, Long reporterId, ReportReason reason, String detail) {
        super(reporterId, reason, detail);
        this.feed = feed;
    }

    public static FeedReport of(Feed feed, Long reporterId, ReportReason reason, String detail) {
        return new FeedReport(feed, reporterId, reason, detail);
    }
}
