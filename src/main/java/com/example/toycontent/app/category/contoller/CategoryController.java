package com.example.toycontent.app.category.contoller;


import com.example.toycontent.app.category.contoller.dto.CategoryRequest;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.ListView;
import com.example.toycontent.app.category.contoller.dto.CategorySearchCondition;
import com.example.toycontent.app.category.service.CategoryService;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;


    @Operation(summary = "카테고리 목록 조회 (페이징)", hidden = true)
    @GetMapping
    public ApiResponse<Page<ListView>> getCategoriesPages(
        @PageableDefault(size = 20) Pageable pageable,
        @ParameterObject CategorySearchCondition.PageSearch condition) {

        Page<ListView> categories = categoryService.getCategoriesPages(pageable, condition);
        return ApiResponse.success(categories);
    }

    @Operation(summary = "인기 카테고리 목록 조회", description = "콘텐츠 등록 수 기준으로 많이 사용된 카테고리를 조회합니다.")
    @GetMapping("/popular")
    public ApiResponse<Page<ListView>> getPopularCategories(
        @PageableDefault(size = 20) Pageable pageable,
        @ParameterObject CategorySearchCondition.PopularSearch condition) {

        Page<ListView> categories = categoryService.getPopularCategories(pageable, condition);
        return ApiResponse.success(categories);
    }

    @Operation(summary = "카테고리 전체 목록 조회", description = "카테고리 전체 목록을 조회합니다.")
    @GetMapping("list")
    public ApiResponse<List<ListView>> getCategories(
            @ParameterObject CategorySearchCondition condition) {

        List<ListView> categories = categoryService.getCategories(condition);
        return ApiResponse.success(categories);
    }

    @Operation(summary = "카테고리 상세 조회", description = "특정 카테고리의 상세 정보를 조회합니다.")
    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse.Detail> getCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId) {
        CategoryResponse.Detail category = categoryService.getCategory(categoryId);
        return ApiResponse.success(category);
    }


    @Operation(summary = "카테고리 생성", description = "새로운 카테고리를 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse.Detail> createCategory(@Valid @RequestBody CategoryRequest.Create request) {
        CategoryResponse.Detail category = categoryService.createCategory(request);
        return ApiResponse.success(category);
    }

    @Operation(summary = "카테고리 수정", description = "기존 카테고리 정보를 수정합니다.")
    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse.Detail> updateCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest.Update request) {

        CategoryResponse.Detail category = categoryService.updateCategory(categoryId, request);
        return ApiResponse.success(category);
    }

    @Operation(summary = "카테고리 삭제", description = "카테고리를 삭제합니다.")
    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.success();
    }


    @Operation(summary = "카테고리 활성화/비활성화", description = "카테고리의 활성화 상태를 토글합니다.")
    @PatchMapping("/{categoryId}/toggle-status")
    public ApiResponse<CategoryResponse.Detail> toggleCategoryStatus(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId) {
        CategoryResponse.Detail category = categoryService.toggleCategoryStatus(categoryId);
        return ApiResponse.success(category);
    }

    @Operation(summary = "카테고리 순서 변경", description = "드래그 앤 드롭으로 카테고리 순서를 변경합니다.")
    @PutMapping("/{categoryId}/reorder")
    public ApiResponse<Void> reorderCategory(
            @Parameter(description = "카테고리 ID") @PathVariable Long categoryId,
            @RequestBody @Valid CategoryRequest.Reorder request) {

        categoryService.reorderCategory(categoryId, request.getTargetPosition());
        return ApiResponse.success();
    }
}