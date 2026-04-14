package com.example.toycontent.support.fixture;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.FeedEvaluation;
import com.example.toycontent.app.feed.domain.Feed;

/**
 * Feed 엔티티 테스트 데이터 빌더.
 * 기본값을 가진 Feed를 빠르게 만들고, 필요한 필드만 오버라이드 가능하게 함.
 */
public class FeedFixture {

  public static final Long DEFAULT_USER_ID = 100L;
  public static final Long DEFAULT_FEED_ID = 1L;

  private FeedFixture() {}

  private static Category defaultCategory() {
    Category parent = Category.builder().id(1L).name("전자제품").build();
    return Category.builder().id(10L).name("노트북").parent(parent).build();
  }

  public static Feed basic() {
    return Feed.builder()
        .id(DEFAULT_FEED_ID)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .review("테스트 한줄평입니다. 맛있어요.")
        .evaluation(FeedEvaluation.GOOD)
        .build();
  }

  public static Feed withId(Long feedId) {
    return Feed.builder()
        .id(feedId)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .review("테스트 한줄평입니다.")
        .evaluation(FeedEvaluation.GOOD)
        .build();
  }

  public static Feed withUserId(Long userId) {
    return Feed.builder()
        .id(DEFAULT_FEED_ID)
        .userId(userId)
        .category(defaultCategory())
        .review("테스트 한줄평입니다.")
        .evaluation(FeedEvaluation.GOOD)
        .build();
  }

  public static Feed withLikeCount(int likeCount) {
    return Feed.builder()
        .id(DEFAULT_FEED_ID)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .review("테스트 한줄평입니다.")
        .evaluation(FeedEvaluation.GOOD)
        .likeCount(likeCount)
        .build();
  }

  public static Feed withViewCounts(int current, int previous) {
    return Feed.builder()
        .id(DEFAULT_FEED_ID)
        .userId(DEFAULT_USER_ID)
        .category(defaultCategory())
        .review("테스트 한줄평입니다.")
        .evaluation(FeedEvaluation.GOOD)
        .viewCount(current)
        .viewCount24hAgo(previous)
        .build();
  }
}
