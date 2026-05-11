package com.example.toycontent.app.product.service;

import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.utils.ProductPopularityCalculator;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductPopularityService {

  private final ProductRepository productRepository;
  private final ProductPopularityCalculator calculator;
  private final EntityManager entityManager;

  private static final int CHUNK_SIZE = 1000;
  private static final int MAX_DIRTY_PER_BATCH = 2000;

  @Transactional
  public int updateDirtyProducts() {
    List<Product> dirtyProducts = productRepository
        .findByPopularityDirtyTrue(PageRequest.of(0, MAX_DIRTY_PER_BATCH));

    if (dirtyProducts.isEmpty()) {
      return 0;
    }

    // TODO: 성능 개선 - JdbcTemplate 배치 처리로 변경 (N개 쿼리 -> 1개 배치 쿼리)
    return updateProductScores(dirtyProducts);
  }

  @Transactional
  public int recalculateAll() {
    int totalCount = 0;
    int page = 0;

    Slice<Product> products;
    do {
      products = productRepository.findByStatusAndIsDeletedFalse(
              ProductStatus.APPROVED,
              PageRequest.of(page++, CHUNK_SIZE)
      );

      if (products.hasContent()) {
        totalCount += updateProductScores(products.getContent());
        entityManager.clear();
      }

    } while (products.hasNext());

    return totalCount;
  }

  /**
   * 상품 점수 계산 및 업데이트
   */
  private int updateProductScores(List<Product> products) {
    Map<Long, Double> scores = calculator.calculateBulk(products);

    productRepository.bulkUpdateScores(scores);

    return scores.size();
  }

  /**
   * 단일 상품 dirty 마킹
   */
  @Transactional
  public void markDirty(Long productId) {
    productRepository.markPopularityDirty(productId);
  }
}
