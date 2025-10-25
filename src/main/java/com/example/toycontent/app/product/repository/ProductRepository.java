package com.example.toycontent.app.product.repository;

import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.querydsl.ProductRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {


}
