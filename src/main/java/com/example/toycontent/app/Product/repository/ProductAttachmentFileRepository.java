package com.example.toycontent.app.Product.repository;

import com.example.toycontent.app.Product.domain.ProductAttachmentFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttachmentFileRepository extends
    JpaRepository<ProductAttachmentFile, Long> {

}
