package com.example.toycontent.app.product.repository;

import com.example.toycontent.app.product.domain.ProductReview;
import com.example.toycontent.app.product.repository.querydsl.ProductReviewRepositoryCustom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long>,
    ProductReviewRepositoryCustom {

  Optional<ProductReview> findByProductId(Long productId);

  long countByProductIdAndCreatedAtAfter(Long id, LocalDateTime recentPeriod);

  @Query("SELECT pr.product.id, COUNT(pr) FROM ProductReview pr WHERE pr.product.id IN :productIds AND pr.createdAt > :since GROUP BY pr.product.id")
  List<Object[]> countByProductIdsAndCreatedAtAfter(@Param("productIds") List<Long> productIds, @Param("since") LocalDateTime since);
}
