package com.example.toycontent.app.category.repository.querydsl;

import com.example.toycontent.app.category.contoller.dto.CategorySearchCondition;
import com.example.toycontent.app.category.domain.Category;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryCustomRepository {

    List<Category> findCategoriesWithSearchCondition(Pageable pageable, CategorySearchCondition condition);

    Long countCategoriesWithSearchCondition(CategorySearchCondition condition);

    List<Category> findCategoriesWithSearchCondition(CategorySearchCondition condition);
}
