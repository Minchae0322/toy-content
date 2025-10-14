package com.example.toycontent.app.Product.repository;

import com.example.toycontent.app.Product.domain.ProductReviewAttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewAttachmentFileRepository extends
    JpaRepository<ProductReviewAttachmentFile, Long> {

}
