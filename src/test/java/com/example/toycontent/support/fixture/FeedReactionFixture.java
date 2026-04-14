package com.example.toycontent.support.fixture;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReaction;

public class FeedReactionFixture {

  public static final Long DEFAULT_ACTOR_USER_ID = 200L;

  private FeedReactionFixture() {}

  public static FeedReaction like(Feed feed) {
    return FeedReaction.builder()
        .feed(feed)
        .userId(DEFAULT_ACTOR_USER_ID)
        .reactionType(FeedReactionType.LIKE)
        .build();
  }

  public static FeedReaction hot(Feed feed) {
    return FeedReaction.builder()
        .feed(feed)
        .userId(DEFAULT_ACTOR_USER_ID)
        .reactionType(FeedReactionType.HOT)
        .build();
  }

  public static FeedReaction of(Feed feed, Long userId, FeedReactionType type) {
    return FeedReaction.builder()
        .feed(feed)
        .userId(userId)
        .reactionType(type)
        .build();
  }
}
