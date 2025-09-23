package com.example.toycontent.app.Product.repository.querydsl;

import com.example.toycontent.app.Product.controller.dto.ProductReviewResponse;
import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import java.util.List;

public interface ProductReviewRepositoryCustom {
  List<ProductReviewResponse.ReviewList> searchProductReviews(Long id,
      ReviewStatus reviewStatus);
}
