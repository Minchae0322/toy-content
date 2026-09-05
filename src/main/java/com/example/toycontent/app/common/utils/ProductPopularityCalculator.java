package com.example.toycontent.app.common.utils;

import com.example.toycontent.app.common.hotscore.HotScoreFormula;
import com.example.toycontent.app.common.hotscore.HotScoreSettings;

import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductReviewRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductPopularityCalculator {

  private final FeedRepository feedRepository;
  private final BattleItemRepository battleItemRepository;
  private final ProductReviewRepository productReviewRepository;

  // 가중치 설정
  private static final double VIEW_WEIGHT = 1.0;
  private static final double LIKE_WEIGHT = 3.0;
  private static final double SHARE_WEIGHT = 5.0;
  private static final double RATING_WEIGHT = 10.0;
  private static final double FEED_WEIGHT = 8.0;
  private static final double BATTLE_WEIGHT = 6.0;
  private static final double REVIEW_WEIGHT = 7.0;

  // 활동 집계 창(일). 피드·배틀·리뷰는 최근 30일치만 참여도에 넣는다
  private static final int RECENT_DAYS = 30;

  /**
   * 상품 인기도 — Reddit식 log10(참여도) + 기준시각/상수(30일).
   *
   * <p>기준시각(anchor)은 "마지막으로 참여가 있었던 시각"이다. 리뷰·피드·배틀 등록처럼 참여가 생기는
   * 순간 {@code now}로 다시 계산하므로, 최근에 활동이 있는 상품이 위로 오고 조용한 상품은 새 활동이
   * 있는 상품에 밀려 내려간다. 시간이 흘러도 저장된 값은 변하지 않아 배치가 필요 없다.</p>
   */
  public double calculate(Product product, LocalDateTime anchor) {
    double engagement = calculateBaseScore(product) + calculateActivityScore(product);
    return HotScoreFormula.score(engagement, anchor, HotScoreSettings.productDivisor());
  }

  /**
   * 벌크 인기도 점수 계산 (N+1 쿼리 방지)
   * 3개 벌크 count 쿼리로 모든 상품의 활동 데이터를 한 번에 조회
   */
  public Map<Long, Double> calculateBulk(List<Product> products) {
    if (products.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Long> productIds = products.stream()
        .map(Product::getId)
        .collect(Collectors.toList());

    LocalDateTime recentPeriod = LocalDateTime.now().minusDays(RECENT_DAYS);

    Map<Long, Long> feedCounts = toCountMap(
        feedRepository.countByProductIdsAndCreatedAtAfter(productIds, recentPeriod));
    Map<Long, Long> battleCounts = toCountMap(
        battleItemRepository.countByProductIdsAndCreatedAtAfter(productIds, recentPeriod));
    Map<Long, Long> reviewCounts = toCountMap(
        productReviewRepository.countByProductIdsAndCreatedAtAfter(productIds, recentPeriod));

    return products.stream()
        .collect(Collectors.toMap(
            Product::getId,
            product -> {
              Long productId = product.getId();
              double baseScore = calculateBaseScoreBulk(product,
                  reviewCounts.getOrDefault(productId, 0L));
              double activityScore =
                  FEED_WEIGHT * feedCounts.getOrDefault(productId, 0L)
                  + BATTLE_WEIGHT * battleCounts.getOrDefault(productId, 0L)
                  + REVIEW_WEIGHT * reviewCounts.getOrDefault(productId, 0L);
              return HotScoreFormula.score(baseScore + activityScore,
                  recalculationAnchor(product), HotScoreSettings.productDivisor());
            }
        ));
  }

  private Map<Long, Long> toCountMap(List<Object[]> results) {
    return results.stream()
        .collect(Collectors.toMap(
            row -> (Long) row[0],
            row -> (Long) row[1]
        ));
  }

  private double calculateBaseScoreBulk(Product product, long recentReviewCount) {
    return VIEW_WEIGHT * nullSafe(product.getViewCount())
        + LIKE_WEIGHT * nullSafe(product.getLikeCount())
        + SHARE_WEIGHT * nullSafe(product.getShareCount())
        + RATING_WEIGHT * nullSafe(product.getAvgRating()) * recentReviewCount;
  }

  /**
   * 기본 점수 계산 (조회수, 좋아요, 공유, 평점)
   */
  private double calculateBaseScore(Product product) {
    LocalDateTime recentPeriod = LocalDateTime.now().minusDays(RECENT_DAYS);
    long recentReviewCount = productReviewRepository
        .countByProductIdAndCreatedAtAfter(product.getId(), recentPeriod);

    return VIEW_WEIGHT * nullSafe(product.getViewCount())
        + LIKE_WEIGHT * nullSafe(product.getLikeCount())
        + SHARE_WEIGHT * nullSafe(product.getShareCount())
        + RATING_WEIGHT * nullSafe(product.getAvgRating()) * recentReviewCount;
  }

  /**
   * 활동 점수 계산 (피드, 배틀, 리뷰)
   */
  private double calculateActivityScore(Product product) {
    LocalDateTime recentPeriod = LocalDateTime.now().minusDays(RECENT_DAYS);
    Long productId = product.getId();

    long feedCount = feedRepository.countByProductIdAndCreatedAtAfter(productId, recentPeriod);
    long battleCount = battleItemRepository.countByProductIdAndCreatedAtAfter(productId, recentPeriod);
    long reviewCount = productReviewRepository.countByProductIdAndCreatedAtAfter(productId, recentPeriod);

    return FEED_WEIGHT * feedCount
        + BATTLE_WEIGHT * battleCount
        + REVIEW_WEIGHT * reviewCount;
  }

  /**
   * 전체 재계산 때 쓰는 기준시각. 마지막 참여 시각(직전 계산 시각)을 유지해야 상수만 바꿨을 때
   * 모든 상품의 최근성이 "지금"으로 초기화되지 않는다.
   */
  private LocalDateTime recalculationAnchor(Product product) {
    if (product.getPopularityCalculatedAt() != null) {
      return product.getPopularityCalculatedAt();
    }
    return getLastActivityTime(product);
  }

  /**
   * 마지막 활동 시간 조회
   */
  private LocalDateTime getLastActivityTime(Product product) {
    return product.getUpdatedAt() != null
        ? product.getUpdatedAt()
        : product.getCreatedAt();
  }

  /**
   * Null-safe 숫자 변환
   */
  private double nullSafe(Number value) {
    return value != null ? value.doubleValue() : 0.0;
  }
}