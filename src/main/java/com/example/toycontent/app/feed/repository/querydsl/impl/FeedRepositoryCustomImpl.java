package com.example.toycontent.app.feed.repository.querydsl.impl;

import static com.example.toycontent.app.feed.domain.QFeed.feed;
import static com.example.toycontent.app.feed.domain.QFeedAttachmentFile.feedAttachmentFile;
import static com.example.toycontent.app.feed.domain.QFeedHashtag.feedHashtag;
import static com.example.toycontent.app.feed.domain.QFeedReaction.feedReaction;
import static com.example.toycontent.app.hashtag.domain.QHashtag.hashtag;
import static com.example.toycontent.app.product.domain.QProduct.product;

import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.HotFeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.QFeed;
import com.example.toycontent.app.feed.repository.querydsl.FeedRepositoryCustom;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryCustomImpl implements FeedRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Feed> findFeedsWithCursor(Search condition) {
    //Feed만 조회 (limit 정확하게 적용)
    List<Feed> feeds = queryFactory
        .selectFrom(feed)
        .distinct()
        .where(
            cursorIdLt(condition.getCursor()),
            categoryEq(condition.getCategoryId(), condition.getCategoryDepth()),
            creatorIdEq(condition.getCreatorId()),
            feed.isDeleted.isFalse()
        )
        .orderBy(feed.id.desc())
        .limit(condition.getSize())
        .fetch();

    if (feeds.isEmpty()) {
      return feeds;
    }

    fetchAssociations(feeds, condition.getReaderId());

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

    if (feeds.isEmpty()) {
      return feeds;
    }


    fetchAssociations(feeds, condition.getReaderId());

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

  /**
   * 연관 데이터 배치 조회
   */
  private void fetchAssociations(List<Feed> feeds, Long readerId) {
    List<Long> feedIds = extractFeedIds(feeds);

    fetchHashtags(feedIds);
    fetchPrimaryAttachments(feedIds);

    if(readerId != null) {
      fetchUserReactions(feedIds, readerId);
    }
  }

  private List<Long> extractFeedIds(List<Feed> feeds) {
    return feeds.stream()
        .map(Feed::getId)
        .toList();
  }

  private void fetchHashtags(List<Long> feedIds) {
    queryFactory
        .selectFrom(feedHashtag)
        .join(feedHashtag.hashtag, hashtag).fetchJoin()
        .where(feedHashtag.feed.id.in(feedIds))
        .fetch();
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



  private void fetchUserReactions(List<Long> feedIds, Long readerId) {
    queryFactory
        .selectFrom(feedReaction)
        .where(
            feedReaction.feed.id.in(feedIds),
            feedReaction.userId.eq(readerId)
        )
        .fetch();
  }


  @Override
  public Page<HotFeedResponse> findAllByHotScore(int recentDays, Pageable pageable) {
    LocalDateTime thresholdDate = LocalDateTime.now().minusDays(recentDays);
    NumberExpression<Double> hotScore = calculateHotScore(feed);

    List<HotFeedResponse> content = queryFactory
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
        .where(feed.createdAt.goe(thresholdDate))
        .orderBy(getOrderSpecifier(pageable.getSort(), hotScore))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    JPAQuery<Long> countQuery = queryFactory
        .select(feed.count())
        .from(feed)
        .where(feed.createdAt.goe(thresholdDate));

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  /**
   * 정렬 조건 변환
   * - 미지정: hotScore DESC → createdAt DESC
   * - hotScore 지정: hotScore → createdAt DESC (tiebreaker)
   * - 그 외: 해당 필드 단독 정렬
   */
  private OrderSpecifier<?>[] getOrderSpecifier(Sort sort, NumberExpression<Double> hotScore) {
    OrderSpecifier<?> hotScoreDesc = new OrderSpecifier<>(Order.DESC, hotScore);
    OrderSpecifier<?> createdAtDesc = new OrderSpecifier<>(Order.DESC, feed.createdAt);

    if (sort.isUnsorted()) {
      return new OrderSpecifier<?>[]{ hotScoreDesc, createdAtDesc };
    }

    return sort.stream()
        .flatMap(order -> {
          Order direction = order.isAscending() ? Order.ASC : Order.DESC;
          return switch (order.getProperty()) {
            case "hotScore" -> Stream.of(
                new OrderSpecifier<>(direction, hotScore), createdAtDesc);
            case "createdAt"    -> Stream.of(new OrderSpecifier<>(direction, feed.createdAt));
            case "viewCount"    -> Stream.of(new OrderSpecifier<>(direction, feed.viewCount));
            case "likeCount"    -> Stream.of(new OrderSpecifier<>(direction, feed.likeCount));
            case "hotCount"     -> Stream.of(new OrderSpecifier<>(direction, feed.hotCount));
            case "commentCount" -> Stream.of(new OrderSpecifier<>(direction, feed.commentCount));
            default -> Stream.of(hotScoreDesc, createdAtDesc);
          };
        })
        .toArray(OrderSpecifier<?>[]::new);
  }


  /**
   * 핫 스코어 계산식
   *
   * hotScore = (likeCount * 2 + hotCount * 3 + viewCount * 0.1) / decayFactor
   * decayFactor = POWER(hoursSinceCreation + 2, 1.5)
   */
  private NumberExpression<Double> calculateHotScore(QFeed feed) {
    // 경과 시간(시간 단위) 계산
    NumberExpression<Long> hoursSinceCreation = Expressions.numberTemplate(
        Long.class,
        "TIMESTAMPDIFF(HOUR, {0}, NOW())",
        feed.createdAt
    );

    // 참여도 점수: (좋아요 * 2 + 핫 * 3 + 조회수 * 0.1)
    NumberExpression<Double> engagementScore = feed.likeCount.multiply(2)
        .add(feed.hotCount.multiply(3))
        .add(feed.viewCount.multiply(0.1))
        .doubleValue();

    // 시간 감쇠 계수: POWER(GREATEST(hoursSinceCreation + 2, 1), 1.5)
    NumberExpression<Double> decayFactor = Expressions.numberTemplate(
        Double.class,
        "POWER(GREATEST({0} + 2, 1), 1.5)",
        hoursSinceCreation
    );

    // 최종 핫 스코어: engagementScore / decayFactor
    return engagementScore.divide(decayFactor);
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
