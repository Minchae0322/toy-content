package com.example.toycontent.app.Product.repository;

import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.Product.repository.querydsl.ProductReviewRepositoryCustom;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long>,
    ProductReviewRepositoryCustom {

}
