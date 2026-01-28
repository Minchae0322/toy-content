package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.controller.dto.FeedCondition;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.repository.querydsl.FeedRepositoryCustom;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepository extends JpaRepository<Feed, Long>, FeedRepositoryCustom {

  /**
   * 비관적 락을 사용한 Feed 조회 (동시성 제어)
   * 리액션 추가/삭제 시 카운트 정합성을 보장하기 위해 사용
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT f FROM Feed f WHERE f.id = :id")
  Optional<Feed> findByIdWithPessimisticLock(@Param("id") Long id);

  /**
   * 검색 조건에 따른 피드 목록 조회 (전체)
   */
  @Query("SELECT f FROM Feed f " +
      "LEFT JOIN FETCH f.category " +
      "LEFT JOIN FETCH f.product " +
      "WHERE (:#{#condition.keyword} IS NULL OR f.review LIKE %:#{#condition.keyword}% OR f.productNameCustom LIKE %:#{#condition.keyword}%) " +
      "AND (:#{#condition.categoryId} IS NULL OR f.category.id = :#{#condition.categoryId}) " +
      "AND (:#{#condition.userId} IS NULL OR f.userId = :#{#condition.userId})")
  List<Feed> findFeedsWithSearchCondition(@Param("condition") FeedCondition condition);

  /**
   * 검색 조건에 따른 피드 개수 조회
   */
  @Query("SELECT COUNT(f) FROM Feed f " +
      "WHERE (:#{#condition.keyword} IS NULL OR f.review LIKE %:#{#condition.keyword}% OR f.productNameCustom LIKE %:#{#condition.keyword}%) " +
      "AND (:#{#condition.categoryId} IS NULL OR f.category.id = :#{#condition.categoryId}) " +
      "AND (:#{#condition.userId} IS NULL OR f.userId = :#{#condition.userId})")
  Long countFeedsWithSearchCondition(@Param("condition") FeedCondition condition);

  /**
   * 사용자별 피드 조회
   */
  List<Feed> findByUserIdOrderByCreatedAtDesc(Long userId);

  /**
   * 카테고리별 피드 조회
   */
  List<Feed> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

  long countByProductIdAndCreatedAtAfter(Long productId, LocalDateTime recentPeriod);

  /**
   * 24시간 전 조회수 스냅샷 저장
   * - 매일 자정 실행
   * - 현재 조회수를 24시간 전 조회수로 복사
   */
  @Modifying
  @Query(value = "UPDATE tb_feed SET view_count_24h_ago = view_count WHERE deleted = 0",
      nativeQuery = true)
  int snapshotViewCount();

  /**
   * 트렌딩 활성화
   * - 24시간 내 조회수 증가량이 threshold 이상인 피드
   */
  @Modifying
  @Query(value = "UPDATE tb_feed SET is_trending = 1 " +
      "WHERE deleted = 0 " +
      "AND is_trending = 0 " +
      "AND view_count_24h_ago IS NOT NULL " +
      "AND (view_count - view_count_24h_ago) >= :threshold",
      nativeQuery = true)
  int markTrending(@Param("threshold") int threshold);

  /**
   * 트렌딩 해제
   * - 24시간 내 조회수 증가량이 threshold 미만인 피드
   */
  @Modifying
  @Query(value = "UPDATE tb_feed SET is_trending = 0 " +
      "WHERE deleted = 0 " +
      "AND is_trending = 1 " +
      "AND (view_count_24h_ago IS NULL OR (view_count - view_count_24h_ago) < :threshold)",
      nativeQuery = true)
  int unmarkTrending(@Param("threshold") int threshold);
}