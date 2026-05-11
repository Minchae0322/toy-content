package com.example.toycontent.app.product.controller;

import com.example.toycontent.app.common.dto.CursorResponse;
import com.example.toycontent.app.product.controller.dto.ProductRequest;
import com.example.toycontent.app.product.controller.dto.ProductResponse;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductBattle;
import com.example.toycontent.app.product.controller.dto.ProductResponse.ProductFeed;
import com.example.toycontent.app.product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.product.service.ProductService;
import com.example.toycontent.app.common.annotation.CheckAdmin;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
        @CurrentUserId(required = false) Long userId) {
        ProductResponse.ProductDetail product = productService.getProduct(id, userId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("")
    @Operation(summary = "제품 목록 조회", description = "제품 목록을 페이징과 정렬 옵션으로 조회합니다.")
    public ResponseEntity<Page<ProductResponse.ProductList>> getAllProducts(
        @ParameterObject @Valid ProductSearchCondition condition,
        @CurrentUserId(required = false) Long userId,
        @ParameterObject @PageableDefault(size = 10, sort = "releaseDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse.ProductList> products = productService.getAllProducts(condition,
            pageable, false);

        return ResponseEntity.ok(products);
    }

    @GetMapping("/my")
    @Operation(summary = "내가 등록한 제품 목록 조회", description = "로그인한 사용자가 등록한 제품 목록을 조회합니다.")
    public ResponseEntity<Page<ProductResponse.ProductList>> getMyProducts(
        @CurrentUserId Long userId,
        @ParameterObject @Valid ProductSearchCondition condition,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductResponse.ProductList> products = productService.getProductsByUserId(userId,
            condition, pageable);
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

    @CheckAdmin
    @Operation(summary = "제품 상태 변경 (관리자)", description = "상품의 판매 상태를 변경합니다.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse.ProductUpdate> updateProductStatus(
        @PathVariable @Parameter(description = "상품 ID") Long id,
        @RequestBody @Valid ProductRequest.ProductStatusRequest request) {
        ProductResponse.ProductUpdate updated = productService.updateProductStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "상품 수정", description = "상품 ID에 해당하는 상품 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse.ProductUpdate> updateProduct(
            @PathVariable @Parameter(description = "상품 ID") Long id,
            @RequestBody @Valid @Parameter(description = "수정할 상품 정보") ProductRequest.ProductUpdate request) {
        ProductResponse.ProductUpdate updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "상품 삭제", description = "상품 ID에 해당하는 상품을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @Parameter(description = "상품 ID") Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

}
