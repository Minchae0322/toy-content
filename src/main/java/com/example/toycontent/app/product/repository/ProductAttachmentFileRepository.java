package com.example.toycontent.app.product.repository;

import com.example.toycontent.app.product.domain.ProductAttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttachmentFileRepository extends
    JpaRepository<ProductAttachmentFile, Long> {

}
