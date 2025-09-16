package com.example.toycontent.app.Product.repository;

import com.example.toycontent.app.Product.domain.ProductReaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReactionRepository extends JpaRepository<ProductReaction, Long> {

  List<ProductReaction> findByUserIdAndProductIdAndIsActiveTrue(Long userId, Long id);
}
