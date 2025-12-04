package com.example.toycontent.app.feed.repository.querydsl;

import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.HotFeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.domain.Feed;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedRepositoryCustom {

  List<Feed> findFeedsWithCursor(Search condition);

  /**
   * 핫 스코어 기반 피드 조회
   */
  Page<HotFeedResponse> findAllByHotScore(int recentDays, Pageable pageable);

  List<Feed> findFollowingFeeds(Following condition);
}
