package com.example.toycontent.app.Product.service;


import com.example.toycontent.app.Product.controller.dto.ProductReactionResponse.ProductUserReaction;
import com.example.toycontent.app.Product.controller.dto.ProductRequest;
import com.example.toycontent.app.Product.controller.dto.ProductResponse;
import com.example.toycontent.app.Product.controller.dto.ProductResponse.ProductCreate;
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
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import jakarta.validation.Valid;
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
    private final CategoryRepository categoryRepository;

    public ProductResponse.ProductCreate createProduct(ProductRequest.ProductCreate productDto, Long userId) {
        Category category = categoryRepository.findById(productDto.getCategoryId()).orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        Product newProduct = productRepository.save(productDto.toEntity(category, null));
        return ProductCreate.of(newProduct);
    }

    public ProductResponse.ProductDetail getProduct(Long id, Long currentUserId) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RestApiException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 로그인한 사용자면 반응 정보 조회, 아니면 기본값 (모두 false) 사용
        ProductUserReaction productUserReaction = Optional.ofNullable(currentUserId)
            .map(userId -> productReactionRepository.findByUserIdAndProductIdAndIsActiveTrue(userId, id))
            .map(ProductUserReaction::of)
            .orElse(ProductUserReaction.createDefault());

        List<ProductReviewResponse.ReviewList> productReviewResponses = productReviewRepository.searchProductReviews(
            product.getId(), ReviewStatus.ACTIVE);

        return ProductDetail.of(product, productUserReaction, productReviewResponses);
    }

    public Page<ProductResponse.ProductList> getAllProducts(
        ProductSearchCondition searchCondition, Pageable pageable, boolean isAdmin) {

        if (!isAdmin) {
            searchCondition.setStatus(ProductStatus.APPROVED);
        }

        List<ProductList> productLists = productRepository.findBySearchCondition(searchCondition, pageable);
        Long totalCount = productRepository.countBySearchCondition(searchCondition);

        return new PageImpl<>(productLists, pageable, totalCount);
    }

    public ProductResponse updateProduct(Long id, ProductResponse productDto) {
        return null;
    }

    public void deleteProduct(Long id) {
    }


}
