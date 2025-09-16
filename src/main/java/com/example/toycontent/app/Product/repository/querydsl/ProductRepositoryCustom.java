package com.example.toycontent.app.Product.repository.querydsl;

import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.Product.controller.dto.ProductSearchCondition;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

  List<ProductList> findBySearchCondition(ProductSearchCondition searchCondition, Pageable pageable);

  Long countBySearchCondition(ProductSearchCondition searchCondition);
}
