package com.example.toycontent.app.feed.repository.querydsl;

import com.example.toycontent.app.feed.controller.dto.FeedSearchCondition;
import com.example.toycontent.app.feed.domain.Feed;
import java.util.List;

public interface FeedRepositoryCustom {

  List<Feed> findFeedsWithCursor(FeedSearchCondition condition);
}
