package com.example.toycontent.app.Product.repository;

import com.example.toycontent.app.Product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
