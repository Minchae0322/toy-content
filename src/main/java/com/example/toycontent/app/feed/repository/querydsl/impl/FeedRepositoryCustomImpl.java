package com.example.toycontent.app.feed.repository.querydsl.impl;

import static com.example.toycontent.app.feed.domain.QFeed.feed;
import static com.example.toycontent.app.feed.domain.QFeedAttachmentFile.feedAttachmentFile;

import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedCursor;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.HotFeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import com.example.toycontent.app.feed.domain.QFeed;
import com.example.toycontent.app.feed.repository.querydsl.FeedRepositoryCustom;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryCustomImpl implements FeedRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Feed> findFeedsWithCursor(Search condition) {
    // 최신순 = created_at DESC, id DESC (id는 같은 시각의 동점 처리). 커서도 같은 두 값.
    // id 단독 정렬은 id 순서 = 작성일 순서를 전제하는데, 적재 데이터에서 그 전제가 깨졌다.
    // 인덱스: idx_feed_created_cursor(deleted, created_at DESC, id DESC)
    List<Feed> feeds = queryFactory
        .selectFrom(feed)
        .distinct()
        .where(
            createdBefore(FeedCursor.parse(condition.getCursor())),
            categoryEq(condition.getCategoryId(), condition.getCategoryDepth()),
            creatorIdEq(condition.getCreatorId()),
            feed.isDeleted.isFalse()
        )
        .orderBy(feed.createdAt.desc(), feed.id.desc())
        .limit(condition.getSize())
        .fetch();

    return feeds;
  }

  @Override
  public List<Feed> findFollowingFeeds(Following condition, List<Long> followings) {
    //Feed만 조회 (limit 정확하게 적용)
    List<Feed> feeds = queryFactory
        .selectFrom(feed)
        .distinct()
        .where(
            creatorIdIn(followings),
            cursorIdLt(condition.getCursor()),
            feed.isDeleted.isFalse()
        )
        .orderBy(feed.id.desc())
        .limit(condition.getSize())
        .fetch();

    return feeds;
  }

  @Override
  public List<Feed> findByProductIdAndIsDeletedNot(Long productId, Boolean isDeleted, Long cursor,
      Integer size) {

    List<Feed> feeds = queryFactory
        .selectFrom(feed)
        .where(
            feed.product.id.eq(productId),
            feed.isDeleted.eq(isDeleted),
            cursorIdLt(cursor)
        )
        .orderBy(feed.id.desc())
        .limit(size)
        .fetch();


    List<Long> feedIds = extractFeedIds(feeds);
    fetchPrimaryAttachments(feedIds);

    return feeds;
  }

  // fetchAssociations(hashtags·reactions 선행 로드)는 2026-08-23 제거 - 결과를 버리는 쿼리였다.
  // 컬렉션·프록시 초기화는 hibernate.default_batch_fetch_size=100이 IN 배치로 처리하고,
  // 조회자 리액션은 service의 findByFeedIdsAndUserId가 (반환값을 실제로 쓰면서) 담당한다.

  private List<Long> extractFeedIds(List<Feed> feeds) {
    return feeds.stream()
        .map(Feed::getId)
        .toList();
  }

  private void fetchPrimaryAttachments(List<Long> feedIds) {
    queryFactory
        .selectFrom(feedAttachmentFile)
        .where(
            feedAttachmentFile.feed.id.in(feedIds),
            feedAttachmentFile.isPrimary.isTrue()
        )
        .fetch();
  }

  @Override
  public List<FeedAttachmentFile> findAttachmentsByFeedIds(List<Long> feedIds) {
    if (feedIds.isEmpty()) {
      return List.of();
    }
    return queryFactory
        .selectFrom(feedAttachmentFile)
        .where(feedAttachmentFile.feed.id.in(feedIds))
        .fetch();
  }

  @Override
  public List<HotFeedResponse> findAllByHotScore(int minViews, Pageable pageable) {
    BooleanExpression minViewsCond = minViews > 0 ? feed.viewCount.goe(minViews) : null;

    return queryFactory
        .select(Projections.fields(
            HotFeedResponse.class,
            feed.id.as("feedId"),
            feed.isTrending,
            feed.productNameCustom.as("productName"),
            Expressions.stringTemplate(
                "CASE WHEN LENGTH({0}) > 50 THEN CONCAT(SUBSTRING({0}, 1, 20), '...') ELSE {0} END",
                feed.review
            ).as("reviewSummary"),
            feed.evaluation.as("feedEvaluation"),
            feed.createdAt.as("createdAt"),
            feed.buyPlace.as("buyPlace"),
            Projections.fields(AttachmentFileResponse.class,
                feedAttachmentFile.id,
                feedAttachmentFile.orgFileNm,
                feedAttachmentFile.fileUrl,
                feedAttachmentFile.fileSize,
                feedAttachmentFile.fileExplain,
                feedAttachmentFile.contentType
            ).as("hotFeedThumbnailDto")
        ))
        .from(feed)
        .leftJoin(feedAttachmentFile)
        .on(feedAttachmentFile.feed.id.eq(feed.id)
            .and(feedAttachmentFile.isPrimary.eq(true)))
        .where(feed.isDeleted.eq(false),
            minViewsCond)
        .orderBy(getOrderSpecifier(pageable.getSort()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  /**
   * 정렬 조건 변환
   * - 미지정: hotScore DESC → createdAt DESC
   * - 그 외: 해당 필드 단독 정렬
   */
  private OrderSpecifier<?>[] getOrderSpecifier(Sort sort) {
    OrderSpecifier<?> hotScoreDesc = new OrderSpecifier<>(Order.DESC, feed.hotScore);
    OrderSpecifier<?> createdAtDesc = new OrderSpecifier<>(Order.DESC, feed.createdAt);

    if (sort.isUnsorted()) {
      return new OrderSpecifier<?>[]{ hotScoreDesc, createdAtDesc };
    }

    return sort.stream()
        .flatMap(order -> {
          Order direction = order.isAscending() ? Order.ASC : Order.DESC;
          return switch (order.getProperty()) {
            case "hotScore"     -> Stream.of(new OrderSpecifier<>(direction, feed.hotScore),
                new OrderSpecifier<>(direction, feed.createdAt));
            case "createdAt"    -> Stream.of(new OrderSpecifier<>(direction, feed.createdAt));
            case "viewCount"    -> Stream.of(
                new OrderSpecifier<>(direction, feed.viewCount.subtract(feed.viewCount24hAgo)));
            case "likeCount"    -> Stream.of(new OrderSpecifier<>(direction, feed.likeCount));
            case "commentCount" -> Stream.of(new OrderSpecifier<>(direction, feed.commentCount));
            default -> Stream.of(hotScoreDesc, createdAtDesc);
          };
        })
        .toArray(OrderSpecifier<?>[]::new);
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

  /** 키셋 커서: created_at < c OR (created_at = c AND id < cid). null이면 첫 페이지. */
  private BooleanExpression createdBefore(FeedCursor cursor) {
    if (cursor == null) {
      return null;
    }
    return feed.createdAt.lt(cursor.createdAt())
        .or(feed.createdAt.eq(cursor.createdAt()).and(feed.id.lt(cursor.id())));
  }

  private BooleanExpression categoryEq(Long categoryId, Integer depth) {
    if (categoryId == null) return null;

    if (depth != null && depth <= 1) {
      // 상위 카테고리 선택 시 자신 + 하위 카테고리 포함
      return feed.category.id.eq(categoryId)
          .or(feed.category.parent.id.eq(categoryId));
    }

    return feed.category.id.eq(categoryId);
  }

  private BooleanExpression creatorIdEq(Long creatorId) {
    return creatorId != null ? feed.userId.eq(creatorId) : null;
  }

  private BooleanExpression creatorIdIn(List<Long> creatorIds) {
    return feed.userId.in(creatorIds);
  }

}
