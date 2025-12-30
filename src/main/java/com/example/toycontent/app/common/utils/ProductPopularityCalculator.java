package com.example.toycontent.app.common.utils;

import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductReviewRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

  // 시간 감쇠 설정
  private static final int RECENT_DAYS = 30;
  private static final double DECAY_HALF_LIFE_DAYS = 7.0;
  private static final double DEFAULT_DECAY = 0.5;

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
    double lambda = Math.log(2) / DECAY_HALF_LIFE_DAYS;

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