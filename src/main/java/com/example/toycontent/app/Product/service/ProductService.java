package com.example.toycontent.app.Product.service;


import com.example.toycontent.app.Product.controller.dto.ProductRequest;
import com.example.toycontent.app.Product.controller.dto.ProductResponse;
import com.example.toycontent.app.Product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
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
    }

    public List<ProductResponse> getAllProducts() {
    }

    public ProductResponse updateProduct(Long id, ProductResponse productDto) {
    }

    public void deleteProduct(Long id) {
    }
}
