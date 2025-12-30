package com.example.toycontent.app.product.controller;

import com.example.toycontent.app.common.dto.CursorResponse;
import com.example.toycontent.app.product.controller.dto.ProductRequest;
import com.example.toycontent.app.product.controller.dto.ProductResponse;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductBattle;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductFeed;
import com.example.toycontent.app.product.controller.dto.ProductReviewRequest;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse;
import com.example.toycontent.app.product.controller.dto.ProductReviewResponse.ReviewCreateResponse;
import com.example.toycontent.app.product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.product.service.ProductService;
import com.example.toycontent.app.common.annotation.CheckAdmin;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.annotation.CurrentUserName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ProductController", description = "상품 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "상품 등록",
            description = "새로운 상품을 등록합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "등록할 상품 정보",
                    required = true)
    )
    @PostMapping
    public ResponseEntity<ProductResponse.ProductCreate> createProduct(
            @RequestBody @Valid ProductRequest.ProductCreate productDto,
            @CurrentUserId Long userId) {
        ProductResponse.ProductCreate created = productService.createProduct(productDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "상품 단건 조회", description = "상품 ID로 상품을 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse.ProductDetail> getProduct(
            @PathVariable @Parameter(description = "상품 ID") Long id,
        @CurrentUserId Long userId) {
        ProductResponse.ProductDetail product = productService.getProduct(id, userId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("")
    @Operation(summary = "제품 목록 조회", description = "제품 목록을 페이징과 정렬 옵션으로 조회합니다.")
    @Parameters({
        @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
        @Parameter(name = "size", description = "페이지 크기 (기본값: 10)", example = "10"),
        @Parameter(name = "sort",
            description = """
                정렬 기준 필드와 방향을 지정합니다.
                
                **사용 가능한 정렬 필드:**
                - `releaseDate` : 제품 출시일 (브랜드 공식 출시일)
                - `createdAt` : 제품 등록 일시 (기본값)
                - `likeCount` : 찜하기 수 (사용자가 관심상품으로 등록한 횟수)
                - `viewCount` : 조회수
                - `name` : 제품명
                - 'popularityScore' : 인기도
                - `updatedAt` : 수정 일시
                
                **정렬 방향:**
                - `ASC` : 오름차순
                - `DESC` : 내림차순 (기본값)
                
                **사용 예시:**
                - `createdAt,DESC` : 등록일 내림차순 (최신순)
                - `likeCount,DESC` : 인기순 (찜하기 많은 순)
                - `releaseDate,ASC` : 출시일 오름차순 (오래된 순)
                """,
            examples = {
                @ExampleObject(name = "최신 등록순", value = "createdAt,DESC"),
                @ExampleObject(name = "출시일 오름차순", value = "releaseDate,ASC"),
                @ExampleObject(name = "조회수 내림차순", value = "viewCount,DESC")
            }
        )
    })
    public ResponseEntity<Page<ProductResponse.ProductList>> getAllProducts(
        @ParameterObject @Valid ProductSearchCondition condition,
        @ParameterObject @PageableDefault(size = 10, sort = "releaseDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse.ProductList> products = productService.getAllProducts(condition,
            pageable, false);

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "상품 피드 목록 조회", description = "상품 ID로 피드 목록을 조회합니다.")
    @GetMapping("/{productId}/feeds")
    public ResponseEntity<CursorResponse<ProductFeed>> findProductFeeds(
        @PathVariable @Parameter(description = "상품 ID") Long productId,
        @RequestParam(required = false) @Parameter(description = "커서 (마지막 피드 ID)") Long cursor,
        @RequestParam(defaultValue = "20") @Parameter(description = "조회 개수") int size,
        @CurrentUserId(required = false) Long userId) {

        CursorResponse<ProductFeed> cursorResponse = productService.findProductFeeds(productId,
            cursor, size);
        return ResponseEntity.ok(cursorResponse);
    }

    @Operation(summary = "상품 배틀 목록 조회", description = "상품 ID로 배틀 목록을 조회합니다.")
    @GetMapping("/{productId}/battles")
    public ResponseEntity<CursorResponse<ProductBattle>> findProductBattles(
        @PathVariable @Parameter(description = "상품 ID") Long productId,
        @RequestParam(required = false) @Parameter(description = "커서 (마지막 피드 ID)") Long cursor,
        @RequestParam(defaultValue = "20") @Parameter(description = "조회 개수") int size,
        @CurrentUserId(required = false) Long userId) {

        CursorResponse<ProductBattle> cursorResponse = productService.findProductBattles(productId,
            cursor, size);
        return ResponseEntity.ok(cursorResponse);
    }




    @GetMapping("/admin/all")
    @CheckAdmin
    @Operation(summary = "전체 제품 목록 조회 (관리자)", description = "승인 대기중인 제품 포함 전체 목록을 조회합니다.")
    public ResponseEntity<Page<ProductResponse.ProductList>> getAllProductsForAdmin(
        @ParameterObject @Valid ProductSearchCondition condition,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 모든 제품 조회 (승인 대기중 포함)
        Page<ProductResponse.ProductList> products = productService.getAllProducts(condition,
            pageable, true);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "상품 수정", description = "상품 ID에 해당하는 상품 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable @Parameter(description = "상품 ID") Long id,
            @RequestBody @Parameter(description = "수정할 상품 정보") ProductResponse productDto) {
        ProductResponse updated = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "상품 삭제", description = "상품 ID에 해당하는 상품을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @Parameter(description = "상품 ID") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 리뷰 작성", description = "상품 ID에 해당하는 상품을 리뷰합니다.")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ProductReviewResponse.ReviewCreateResponse> createProductReview(
        @PathVariable @Parameter(description = "상품 ID") Long id,
        @RequestBody @Valid ProductReviewRequest.CreateReview createReviewDto,
        @CurrentUserId Long userId,
        @CurrentUserName String userName) {
        ReviewCreateResponse createdReview = productService.createReview(id, createReviewDto,
            userId, userName);
        return ResponseEntity.ok(createdReview);
    }

    @Operation(summary = "상품 리뷰 목록 조회", description = "상품 ID에 해당하는 리뷰 목록을 조회합니다.")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ProductReviewResponse.ReviewList>> getProductReviews(
        @PathVariable @Parameter(description = "상품 ID") Long id,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<ProductReviewResponse.ReviewList> reviews = productService.getReviews(id, pageable);
        return ResponseEntity.ok(reviews);
    }
}
