package com.example.toycontent.app.product.repository;

import com.example.toycontent.app.product.domain.ProductReview;
import com.example.toycontent.app.product.repository.querydsl.ProductReviewRepositoryCustom;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long>,
    ProductReviewRepositoryCustom {

  Optional<ProductReview> findByProductId(Long productId);

  long countByProductIdAndCreatedAtAfter(Long id, LocalDateTime recentPeriod);
}
