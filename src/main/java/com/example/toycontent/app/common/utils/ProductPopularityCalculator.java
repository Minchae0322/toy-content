package com.example.toycontent.app.common.utils;

import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductReviewRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

  // 시간 감쇠 설정
  private static final int RECENT_DAYS = 30;
  private static final double DEFAULT_DECAY = 0.5;

  /**
   * 인기도 감쇠 반감기(일). 피드보다 길게 잡아 한번 인기를 끈 상품이 더 오래 노출되도록 함.
   * 예: 14일이면 Day 14에 50%, Day 30에 약 23%까지 유지.
   */
  @Value("${product.popularity.decay-half-life-days:14}")
  private double decayHalfLifeDays;

  /**
   * 상품의 인기도 점수 계산
   */
  public double calculate(Product product) {
    double baseScore = calculateBaseScore(product);
    double activityScore = calculateActivityScore(product);
    double timeDecay = calculateTimeDecay(product);

    return (baseScore + activityScore) * timeDecay;
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
              double timeDecay = calculateTimeDecay(product);
              return (baseScore + activityScore) * timeDecay;
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
   * 시간 감쇠 계산 (지수 감쇠)
   * - 최근 활동일수록 1에 가깝고, 오래될수록 0에 가까워짐
   */
  private double calculateTimeDecay(Product product) {
    LocalDateTime lastActivity = getLastActivityTime(product);

    if (lastActivity == null) {
      return DEFAULT_DECAY;
    }

    long daysSinceActivity = ChronoUnit.DAYS.between(lastActivity, LocalDateTime.now());
    double lambda = Math.log(2) / decayHalfLifeDays;

    return Math.exp(-lambda * daysSinceActivity);
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