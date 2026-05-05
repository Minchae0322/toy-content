package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.domain.FeedReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedReportRepository extends JpaRepository<FeedReport, Long> {

    boolean existsByFeedIdAndReporterId(Long feedId, Long reporterId);
}
