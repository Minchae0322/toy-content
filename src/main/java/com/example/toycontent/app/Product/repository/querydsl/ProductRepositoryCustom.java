package com.example.toycontent.app.Product.repository.querydsl;

import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.Product.controller.dto.ProductSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

  Page<ProductList> findBySearchCondition(ProductSearchCondition searchCondition, Pageable pageable);
}
