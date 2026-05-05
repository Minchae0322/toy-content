package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedReportRequest;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReport;
import com.example.toycontent.app.feed.repository.FeedReportRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedReportService {

    private final FeedRepository feedRepository;
    private final FeedReportRepository feedReportRepository;

    public Long report(Long feedId, Long reporterId, FeedReportRequest request) {
        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

        if (feed.getUserId().equals(reporterId)) {
            throw new RestApiException(FeedErrorCode.FEED_REPORT_SELF);
        }

        if (feedReportRepository.existsByFeedIdAndReporterId(feedId, reporterId)) {
            throw new RestApiException(FeedErrorCode.FEED_REPORT_DUPLICATED);
        }

        FeedReport report = FeedReport.of(feed, reporterId, request.getReason(), request.getDetail());
        return feedReportRepository.save(report).getId();
    }
}
