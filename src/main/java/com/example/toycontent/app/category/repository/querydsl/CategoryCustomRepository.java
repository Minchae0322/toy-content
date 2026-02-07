package com.example.toycontent.app.category.repository.querydsl;

import com.example.toycontent.app.category.contoller.dto.CategoryResponse.ListView;
import com.example.toycontent.app.category.contoller.dto.CategorySearchCondition;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.dto.CategoryCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryCustomRepository {

    List<Category> findCategoriesWithSearchCondition(Pageable pageable, CategorySearchCondition condition);

    Long countCategoriesWithSearchCondition(CategorySearchCondition condition);

    List<Category> findCategoriesWithSearchCondition(CategorySearchCondition condition);

    Page<CategoryCountDto> findPopularByBattle(Pageable pageable, CategorySearchCondition.PopularSearch condition);
    Page<CategoryCountDto> findPopularByFeed(Pageable pageable, CategorySearchCondition.PopularSearch condition);
    Page<CategoryCountDto> findPopularByProduct(Pageable pageable, CategorySearchCondition.PopularSearch condition);
}
