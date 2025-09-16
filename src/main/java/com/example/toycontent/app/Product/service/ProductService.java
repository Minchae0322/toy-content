package com.example.toycontent.app.Product.service;


import com.example.toycontent.app.Product.controller.dto.ProductReactionResponse.ProductUserReaction;
import com.example.toycontent.app.Product.controller.dto.ProductRequest;
import com.example.toycontent.app.Product.controller.dto.ProductResponse;
import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductDetail;
import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductList;
import com.example.toycontent.app.Product.controller.dto.ProductReviewResponse;
import com.example.toycontent.app.Product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.Product.domain.ProductReaction;
import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.Product.repository.ProductReactionRepository;
import com.example.toycontent.app.Product.repository.ProductRepository;
import com.example.toycontent.app.Product.repository.ProductReviewRepository;
import com.example.toycontent.app.Product.repository.querydsl.impl.ProductRepositoryCustomImpl;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReactionRepository productReactionRepository;
    private final ProductReviewRepository productReviewRepository;

    public ProductResponse createProduct(ProductRequest productDto, Long userId) {
        return null;
    }

    public ProductResponse.ProductDetail getProduct(Long id, Long currentUserId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RestApiException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 로그인한 사용자면 반응 정보 조회, 아니면 기본값 (모두 false) 사용
        ProductUserReaction productUserReaction = Optional.ofNullable(currentUserId)
            .map(userId -> productReactionRepository.findByUserIdAndProductIdAndIsActiveTrue(userId, id))
            .map(ProductUserReaction::of)
            .orElse(ProductUserReaction.createDefault());

        List<ProductReviewResponse.ReviewList> productReviewResponses = productReviewRepository.findByProduct_IdAndStatus(
            product.getId(), ReviewStatus.ACTIVE);

        return ProductDetail.of(product, productUserReaction, productReviewResponses);
    }

    public Page<ProductResponse.ProductList> getAllProducts(ProductSearchCondition searchCondition,
        Pageable pageable) {
        // 검색 조건에 맞는 제품 목록 조회
        List<ProductList> productLists = productRepository.findBySearchCondition(searchCondition,
            pageable);

        // 전체 개수 조회
        Long totalCount = productRepository.countBySearchCondition(searchCondition);

        return new PageImpl<>(productLists, pageable, totalCount);
    }

    public ProductResponse updateProduct(Long id, ProductResponse productDto) {
        return null;
    }

    public void deleteProduct(Long id) {
    }
}
