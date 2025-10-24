package com.example.toycontent.app.feed.repository.querydsl.impl;

import static com.example.toycontent.app.feed.domain.QFeed.feed;
import static com.example.toycontent.app.feed.domain.QFeedAttachmentFile.feedAttachmentFile;
import static com.example.toycontent.app.feed.domain.QFeedHashtag.feedHashtag;
import static com.example.toycontent.app.hashtag.domain.QHashtag.hashtag;

import com.example.toycontent.app.feed.controller.dto.FeedSearchCondition;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.repository.querydsl.FeedRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryCustomImpl implements FeedRepositoryCustom {

  private JPAQueryFactory queryFactory;

  @Override
  public List<Feed> findFeedsWithCursor(FeedSearchCondition condition) {
    // 1. Feed만 조회 (limit 정확하게 적용)
    List<Feed> feeds = queryFactory
        .selectFrom(feed)
        .distinct()
        .where(
            cursorIdLt(condition.getCursor()),
            categoryEq(condition.getCategoryId())
        )
        .orderBy(feed.id.desc())
        .limit(condition.getSize())
        .fetch();

    if (feeds.isEmpty()) {
      return feeds;
    }

    fetchAssociations(feeds);

    return feeds;
  }

  /**
   * 연관 데이터 배치 조회
   */
  private void fetchAssociations(List<Feed> feeds) {
    List<Long> feedIds = feeds.stream()
        .map(Feed::getId)
        .toList();

    // 해시태그
    queryFactory
        .selectFrom(feedHashtag)
        .join(feedHashtag.hashtag, hashtag).fetchJoin()
        .where(feedHashtag.feed.id.in(feedIds))
        .fetch();

    // 대표 이미지만
    queryFactory
        .selectFrom(feedAttachmentFile)
        .where(
            feedAttachmentFile.feed.id.in(feedIds),
            feedAttachmentFile.isPrimary.isTrue()
        )
        .fetch();
  }

  /**
   * 커서 조건 - ID가 cursor보다 작은 것
   * (최신순 정렬이므로 lt 사용)
   */
  private BooleanExpression cursorIdLt(Long cursor) {
    return Optional.ofNullable(cursor)
        .map(feed.id::lt)
        .orElse(null);
  }

  private BooleanExpression categoryEq(Long categoryId) {
    return categoryId != null ? feed.category.id.eq(categoryId) : null;
  }

}
