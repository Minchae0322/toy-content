package com.example.toycontent.app.category.repository;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.querydsl.CategoryCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryCustomRepository {

    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Category c")
    Integer findMaxSortOrder();

    /**
     * sortOrder 내림차순으로 조회 (아래로 밀어내기용)
     */
    List<Category> findBySortOrderBetweenOrderBySortOrderDesc(Integer start, Integer end);



}
