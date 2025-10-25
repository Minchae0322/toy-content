package com.example.toycontent.app.product.repository.querydsl;

import com.example.toycontent.app.product.controller.dto.ProductReviewResponse;
import com.example.toycontent.app.product.domain.ProductReview;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductReviewRepositoryCustom {
  List<ProductReviewResponse.ReviewList> searchProductReviews(Long id,
      ReviewStatus reviewStatus);

  List<ProductReview> findProductReviews(Long id,
      ReviewStatus reviewStatus, Pageable pageable);
}
