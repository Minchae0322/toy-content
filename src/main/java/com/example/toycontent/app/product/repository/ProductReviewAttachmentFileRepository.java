package com.example.toycontent.app.product.repository;

import com.example.toycontent.app.product.domain.ProductReviewAttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewAttachmentFileRepository extends
    JpaRepository<ProductReviewAttachmentFile, Long> {

}
