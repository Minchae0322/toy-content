package com.example.toycontent.app.category.repository;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.querydsl.CategoryCustomRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryCustomRepository {

    /**
     * 최상위 카테고리들(parent가 null) 중 최대 정렬 순서 조회
     */
    @Query("SELECT MAX(c.sortOrder) FROM Category c WHERE c.parent IS NULL")
    Optional<Integer> findMaxSortOrderByParentIsNull();

    /**
     * 특정 부모를 가진 카테고리들 중 최대 정렬 순서 조회
     */
    @Query("SELECT MAX(c.sortOrder) FROM Category c WHERE c.parent.id = :parentId")
    Optional<Integer> findMaxSortOrderByParentId(@Param("parentId") Long parentId);

    // 같은 부모를 가진 카테고리 개수
    Long countByParentId(Long parentId);
    Long countByParentIsNull();

    // 같은 부모를 가진 특정 범위의 카테고리 조회
    List<Category> findByParentIdAndSortOrderBetweenOrderBySortOrderDesc(
        Long parentId, Integer start, Integer end);

    List<Category> findByParentIsNullAndSortOrderBetweenOrderBySortOrderDesc(
        Integer start, Integer end);



}
