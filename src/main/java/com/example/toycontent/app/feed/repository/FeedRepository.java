package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.controller.dto.FeedSearchCondition;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.repository.querydsl.FeedRepositoryCustom;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

  /**
   * 검색 조건에 따른 피드 목록 조회 (페이징)
   */
  @Query("SELECT f FROM Feed f " +
      "LEFT JOIN FETCH f.category " +
      "LEFT JOIN FETCH f.product " +
      "WHERE (:#{#condition.keyword} IS NULL OR f.review LIKE %:#{#condition.keyword}% OR f.productNameCustom LIKE %:#{#condition.keyword}%) " +
      "AND (:#{#condition.categoryId} IS NULL OR f.category.id = :#{#condition.categoryId}) " +
      "AND (:#{#condition.userId} IS NULL OR f.userId = :#{#condition.userId})")
  List<Feed> findFeedsWithSearchCondition(Pageable pageable, @Param("condition") FeedSearchCondition condition);

  /**
   * 검색 조건에 따른 피드 목록 조회 (전체)
   */
  @Query("SELECT f FROM Feed f " +
      "LEFT JOIN FETCH f.category " +
      "LEFT JOIN FETCH f.product " +
      "WHERE (:#{#condition.keyword} IS NULL OR f.review LIKE %:#{#condition.keyword}% OR f.productNameCustom LIKE %:#{#condition.keyword}%) " +
      "AND (:#{#condition.categoryId} IS NULL OR f.category.id = :#{#condition.categoryId}) " +
      "AND (:#{#condition.userId} IS NULL OR f.userId = :#{#condition.userId})")
  List<Feed> findFeedsWithSearchCondition(@Param("condition") FeedSearchCondition condition);

  /**
   * 검색 조건에 따른 피드 개수 조회
   */
  @Query("SELECT COUNT(f) FROM Feed f " +
      "WHERE (:#{#condition.keyword} IS NULL OR f.review LIKE %:#{#condition.keyword}% OR f.productNameCustom LIKE %:#{#condition.keyword}%) " +
      "AND (:#{#condition.categoryId} IS NULL OR f.category.id = :#{#condition.categoryId}) " +
      "AND (:#{#condition.userId} IS NULL OR f.userId = :#{#condition.userId})")
  Long countFeedsWithSearchCondition(@Param("condition") FeedSearchCondition condition);

  /**
   * 사용자별 피드 조회
   */
  List<Feed> findByUserIdOrderByCreatedAtDesc(Long userId);

  /**
   * 카테고리별 피드 조회
   */
  List<Feed> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
}