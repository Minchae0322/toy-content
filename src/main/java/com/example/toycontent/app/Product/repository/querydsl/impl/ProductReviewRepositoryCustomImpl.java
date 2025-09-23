package com.example.toycontent.app.Product.repository.querydsl.impl;

import static com.example.toycontent.app.Product.domain.QProductReview.productReview;

import com.example.toycontent.app.Product.controller.dto.ProductReviewResponse.ReviewList;
import com.example.toycontent.app.Product.repository.querydsl.ProductReviewRepositoryCustom;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductReviewRepositoryCustomImpl implements ProductReviewRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  @Override
  public List<ReviewList> searchProductReviews(Long id, ReviewStatus reviewStatus) {
    return queryFactory
        .select(Projections.fields(
            ReviewList.class,
            productReview.id,
            productReview.creatorId,
            productReview.creatorName,
            productReview.rating,
            productReview.comment,
            productReview.status,
            productReview.likeCount,
            productReview.reportCount,
            productReview.createdAt,
            productReview.updatedAt
        ))
        .from(productReview)
        .where(
            productReview.product.id.eq(id),
            productReview.status.eq(reviewStatus)
        )
        .orderBy(productReview.createdAt.desc())
        .fetch();

  }
}
