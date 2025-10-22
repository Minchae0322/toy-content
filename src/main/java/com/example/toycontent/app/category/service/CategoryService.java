package com.example.toycontent.app.category.service;

import com.example.toycontent.app.category.contoller.dto.CategoryRequest;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.ListView;
import com.example.toycontent.app.category.contoller.dto.CategorySearchCondition;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Page<ListView> getCategoriesPages(Pageable pageable, CategorySearchCondition condition) {
        List<Category> categories = categoryRepository.findCategoriesWithSearchCondition(pageable, condition);
        Long totalCount = categoryRepository.countCategoriesWithSearchCondition(condition);

        return new PageImpl<>(
                categories.stream()
                        .map(ListView::from)
                        .toList(),
                pageable, totalCount);
    }


    public List<ListView> getCategories(CategorySearchCondition condition) {
        List<Category> categories = categoryRepository.findCategoriesWithSearchCondition(condition);

        return categories.stream()
                .map(ListView::from)
                .toList();
    }


    public CategoryResponse.Detail getCategory(Long categoryId) {
        Category category = findCategoryById(categoryId);
        return CategoryResponse.Detail.from(category);
    }

    @Transactional
    public CategoryResponse.Detail createCategory(CategoryRequest.Create request) {

        Integer nextSortOrder = getNextSortOrder(request.getParentId());
        Category category = request.toEntity(nextSortOrder);

        Optional.ofNullable(request.getParentId())
            .flatMap(categoryRepository::findById)
            .map(this::validateAndReturn)
            .ifPresent(category::setParent);

        Category savedCategory = categoryRepository.save(category);

        return CategoryResponse.Detail.from(savedCategory);
    }

    private Category validateAndReturn(Category parent) {
        if (!parent.getIsActive()) {
            throw new RestApiException(CategoryErrorCode.PARENT_CATEGORY_INACTIVE);
        }
        if (parent.getDepth() >= 2) {
            throw new RestApiException(CategoryErrorCode.MAX_DEPTH_EXCEEDED);
        }
        return parent;
    }

    /**
     * 같은 부모를 가진 카테고리들 중 다음 정렬 순서를 반환합니다.
     *
     * @param parentId 부모 카테고리 ID (null이면 최상위 카테고리)
     * @return 다음 정렬 순서
     */
    private synchronized Integer getNextSortOrder(Long parentId) {
        return Optional.ofNullable(parentId)
            .map(categoryRepository::findMaxSortOrderByParentId)
            .orElseGet(categoryRepository::findMaxSortOrderByParentIsNull)
            .map(max -> max + 1)
            .orElse(1);
    }

    @Transactional
    public CategoryResponse.Detail updateCategory(Long categoryId, CategoryRequest.Update request) {
        Category category = findCategoryById(categoryId);
        category.update(request);

        return CategoryResponse.Detail.from(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findCategoryById(categoryId);
        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryResponse.Detail toggleCategoryStatus(Long categoryId) {
        Category category = findCategoryById(categoryId);
        category.toggleStatus();
        return CategoryResponse.Detail.from(category);
    }

    /**
     * 카테고리를 드래그하여 같은 레벨 내에서 특정 위치에 삽입하는 메서드
     *
     * @param categoryId 이동할 카테고리 ID
     * @param targetPosition 목표 위치 (1부터 시작)
     */
    @Transactional
    public void reorderCategory(Long categoryId, Integer targetPosition) {
        Category category = findCategoryById(categoryId);
        validateTargetPosition(targetPosition);

        Integer currentPosition = category.getSortOrder();
        if (currentPosition.equals(targetPosition)) {
            return;
        }

        Long parentId = Optional.ofNullable(category.getParent())
            .map(Category::getId)
            .orElse(null);

        // 같은 레벨(부모)의 전체 카테고리 개수 확인
        long totalCount = countSiblings(parentId);
        int adjustedTargetPosition = Math.min(targetPosition, (int) totalCount);

        if (currentPosition < adjustedTargetPosition) {
            moveItemsUp(parentId, currentPosition + 1, adjustedTargetPosition);
        } else {
            moveItemsDown(parentId, adjustedTargetPosition, currentPosition - 1);
        }

        category.setSortOrder(adjustedTargetPosition);
        categoryRepository.save(category);
    }

    /**
     * 같은 부모를 가진 카테고리 개수 조회
     */
    private long countSiblings(Long parentId) {
        return Optional.ofNullable(parentId)
            .map(categoryRepository::countByParentId)
            .orElseGet(categoryRepository::countByParentIsNull);
    }

    /**
     * 특정 범위의 카테고리들을 위로 한 칸씩 이동 (같은 부모 내에서만)
     */
    private void moveItemsUp(Long parentId, Integer startPosition, Integer endPosition) {
        List<Category> categories = findCategoriesByParentAndRange(parentId, startPosition, endPosition);

        categories.forEach(cat -> cat.setSortOrder(cat.getSortOrder() - 1));
        categoryRepository.saveAll(categories);
    }

    /**
     * 특정 범위의 카테고리들을 아래로 한 칸씩 이동 (같은 부모 내에서만)
     */
    private void moveItemsDown(Long parentId, Integer startPosition, Integer endPosition) {
        List<Category> categories = findCategoriesByParentAndRange(parentId, startPosition, endPosition);

        categories.forEach(cat -> cat.setSortOrder(cat.getSortOrder() + 1));
        categoryRepository.saveAll(categories);
    }

    /**
     * 같은 부모를 가진 특정 범위의 카테고리 조회
     */
    private List<Category> findCategoriesByParentAndRange(Long parentId, Integer startPosition, Integer endPosition) {
        return Optional.ofNullable(parentId)
            .map(id -> categoryRepository.findByParentIdAndSortOrderBetweenOrderBySortOrderDesc(
                id, startPosition, endPosition))
            .orElseGet(() -> categoryRepository.findByParentIsNullAndSortOrderBetweenOrderBySortOrderDesc(
                startPosition, endPosition));
    }

    private void validateTargetPosition(Integer targetPosition) {
        if (targetPosition < 1) {
            throw new RestApiException(CategoryErrorCode.INVALID_SORT_ORDER);
        }
    }


    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

}
