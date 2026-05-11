package com.example.toycontent.app.product.controller;

import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.annotation.CurrentUserName;
import com.example.toycontent.app.product.controller.dto.ProductReviewRequest;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse.ReviewCreateResponse;
import com.example.toycontent.app.product.service.ProductReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ProductReviewController", description = "상품 리뷰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/products/{productId}/reviews")
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @Operation(summary = "상품 리뷰 작성", description = "상품 ID에 해당하는 상품을 리뷰합니다.")
    @PostMapping
    public ResponseEntity<ReviewCreateResponse> createProductReview(
        @PathVariable @Parameter(description = "상품 ID") Long productId,
        @RequestBody @Valid ProductReviewRequest.CreateReview createReviewDto,
        @CurrentUserId Long userId,
        @CurrentUserName String userName) {
        ReviewCreateResponse createdReview = productReviewService.createReview(productId,
            createReviewDto, userId, userName);
        return ResponseEntity.ok(createdReview);
    }

    @Operation(summary = "상품 리뷰 목록 조회", description = "상품 ID에 해당하는 리뷰 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ProductReviewResponse.ReviewList>> getProductReviews(
        @PathVariable @Parameter(description = "상품 ID") Long productId,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<ProductReviewResponse.ReviewList> reviews = productReviewService.getReviews(productId,
            pageable);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "리뷰 수정", description = "작성자 본인만 자신의 리뷰를 수정할 수 있습니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<Void> updateReview(
        @PathVariable @Parameter(description = "상품 ID") Long productId,
        @PathVariable @Parameter(description = "리뷰 ID") Long reviewId,
        @RequestBody @Valid ProductReviewRequest.UpdateReview request,
        @CurrentUserId Long userId) {
        productReviewService.updateReview(productId, reviewId, request, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "리뷰 삭제", description = "작성자 본인만 자신의 리뷰를 삭제할 수 있습니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @PathVariable @Parameter(description = "상품 ID") Long productId,
        @PathVariable @Parameter(description = "리뷰 ID") Long reviewId,
        @CurrentUserId Long userId) {
        productReviewService.deleteReview(productId, reviewId, userId);
        return ResponseEntity.noContent().build();
    }
}
