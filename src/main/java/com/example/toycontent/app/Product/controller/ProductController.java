package com.example.toycontent.app.Product.controller;

import com.example.toycontent.app.Product.controller.dto.ProductRequest;
import com.example.toycontent.app.Product.controller.dto.ProductResponse;
import com.example.toycontent.app.Product.controller.dto.ProductSearchCondition;
import com.example.toycontent.app.Product.service.ProductService;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

import java.util.List;

@Tag(name = "ProductController", description = "상품 API")
@RestController
@RequiredArgsConstructor
@RequestMapping
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
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody @Valid ProductRequest productDto,
            @CurrentUserId Long userId) {
        ProductResponse created = productService.createProduct(productDto, userId);
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

    @GetMapping
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
                @ExampleObject(name = "인기순 (찜하기)", value = "likeCount,DESC"),
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
            pageable);

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
}
