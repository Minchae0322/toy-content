package com.example.toycontent.app.product.service;

import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.hotscore.domain.HotScoreSettings;
import com.example.toycontent.app.common.utils.ProductPopularityCalculator;
import com.example.toycontent.app.config.CacheConfig;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

/**
 * 제품 인기도.
 *
 * <ul>
 *   <li>{@link #refresh(Long)} — 등록·리뷰·피드·배틀처럼 참여가 생길 때 그 상품 한 건만 다시 계산한다.
 *       기준시각을 지금으로 잡으므로 "최근에 활동이 있는 상품"이 위로 온다.</li>
 *   <li>{@link #recalculateAll()} — 수동 실행 전용. 시간 상수를 바꾼 뒤 저장된 점수를 새 기준으로 맞춘다.
 *       기준시각은 각 상품의 직전 계산 시각을 유지한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPopularityService {

  private final ProductRepository productRepository;
  private final ProductPopularityCalculator calculator;
  private final EntityManager entityManager;

  private static final int CHUNK_SIZE = 1000;

  /** 참여가 생긴 상품 한 건 재계산. 승인되지 않았거나 삭제된 상품은 건너뛴다. */
  @Transactional
  public void refresh(Long productId) {
    if (productId == null) {
      return;
    }
    productRepository.findById(productId)
        .filter(p -> p.getStatus() == ProductStatus.APPROVED && !Boolean.TRUE.equals(p.getIsDeleted()))
        .ifPresent(p -> productRepository.updatePopularityScore(
            productId, calculator.calculate(p, LocalDateTime.now())));
  }

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.POPULAR_PRODUCTS, allEntries = true)
  public int recalculateAll() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    int totalCount = 0;
    int page = 0;
    Slice<Product> products;
    do {
      products = productRepository.findByStatusAndIsDeletedFalse(
          ProductStatus.APPROVED, PageRequest.of(page++, CHUNK_SIZE));
      if (products.hasContent()) {
        Map<Long, Double> scores = calculator.calculateBulk(products.getContent());
        productRepository.bulkUpdateScores(scores);
        totalCount += scores.size();
        entityManager.clear();
      }
    } while (products.hasNext());
    stopWatch.stop();
    log.info("[hot-score] 제품 전체 재계산 완료 - {}건, divisor={}s, {}ms",
        totalCount, HotScoreSettings.productDivisor(), stopWatch.getTotalTimeMillis());
    return totalCount;
  }
}
