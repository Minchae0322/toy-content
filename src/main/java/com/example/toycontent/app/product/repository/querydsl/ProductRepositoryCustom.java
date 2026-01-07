package com.example.toycontent.app.product.repository.querydsl;

import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.product.controller.dto.ProductSearchCondition;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

  List<ProductList> findBySearchCondition(ProductSearchCondition searchCondition, Pageable pageable);

  Long countBySearchCondition(ProductSearchCondition searchCondition);

  List<ProductList> findByUserIdAndSearchCondition(Long userId, ProductSearchCondition condition, Pageable pageable);

  Long countByUserIdAndSearchCondition(Long userId, ProductSearchCondition condition);
}
