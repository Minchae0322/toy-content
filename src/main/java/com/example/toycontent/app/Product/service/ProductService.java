package com.example.toycontent.app.Product.service;


import com.example.toycontent.app.Product.controller.dto.ProductRequest;
import com.example.toycontent.app.Product.controller.dto.ProductResponse;
import com.example.toycontent.app.Product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;


    public ProductResponse createProduct(ProductRequest productDto, Long userId) {
        return null;
    }

    public ProductResponse getProduct(Long id) {
        return null;
    }

    public Page<ProductResponse> getAllProducts() {
        return null;
    }

    public ProductResponse updateProduct(Long id, ProductResponse productDto) {
        return null;
    }

    public void deleteProduct(Long id) {
    }
}
