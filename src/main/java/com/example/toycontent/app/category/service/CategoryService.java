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
        Integer nextSortOrder = getNextSortOrder();

        Category savedCategory = categoryRepository.save(request.toEntity(nextSortOrder));

        return CategoryResponse.Detail.from(savedCategory);
    }

    private synchronized Integer getNextSortOrder() {
        Integer maxOrder = categoryRepository.findMaxSortOrder();
        return maxOrder + 1;
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
     * 카테고리를 드래그하여 특정 위치에 삽입하는 메서드
     * @param categoryId 이동할 카테고리 ID
     * @param targetPosition 목표 위치 (1부터 시작)
     */
    @Transactional
    public void reorderCategory(Long categoryId, Integer targetPosition) {
        Category category = findCategoryById(categoryId);
        Integer currentPosition = category.getSortOrder();

        if (targetPosition < 1) {
            throw new RestApiException(CategoryErrorCode.INVALID_SORT_ORDER);
        }

        if (currentPosition.equals(targetPosition)) {
            return;
        }

        // 전체 카테고리 개수 확인
        long totalCount = categoryRepository.count();
        if (targetPosition > totalCount) {
            targetPosition = (int) totalCount; // 마지막 위치로 조정
        }

        if (currentPosition < targetPosition) {
            // 아래쪽으로 이동: 현재 위치와 목표 위치 사이의 카테고리들을 위로 당김
            moveItemsUp(currentPosition + 1, targetPosition);
        } else {
            // 위쪽으로 이동: 목표 위치와 현재 위치 사이의 카테고리들을 아래로 밀어냄
            moveItemsDown(targetPosition, currentPosition - 1);
        }

        // 이동할 카테고리를 목표 위치에 설정
        category.setSortOrder(targetPosition);
        categoryRepository.save(category);
    }

    /**
     * 특정 범위의 카테고리들을 위로 한 칸씩 이동
     */
    private void moveItemsUp(Integer startPosition, Integer endPosition) {
        List<Category> categories = categoryRepository
                .findBySortOrderBetweenOrderBySortOrderDesc(startPosition, endPosition);

        for (Category cat : categories) {
            cat.setSortOrder(cat.getSortOrder() - 1);
        }
        categoryRepository.saveAll(categories);
    }

    /**
     * 특정 범위의 카테고리들을 아래로 한 칸씩 이동
     */
    private void moveItemsDown(Integer startPosition, Integer endPosition) {
        List<Category> categories = categoryRepository
                .findBySortOrderBetweenOrderBySortOrderDesc(startPosition, endPosition);

        for (Category cat : categories) {
            cat.setSortOrder(cat.getSortOrder() + 1);
        }
        categoryRepository.saveAll(categories);
    }


    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

}
