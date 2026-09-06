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
   * 조회수 원자 증가. 엔티티 로드·더티 체킹 없이 단문 UPDATE 하나로 처리한다.
   * 읽기 경로(getFeed)가 readOnly 트랜잭션이 될 수 있도록 쓰기를 여기로 분리했다.
   */
  /**
   * 조회수 +1 과 함께 hot_score를 같은 문장에서 갱신한다.
   * MySQL은 SET을 왼쪽부터 순서대로 평가하므로 hot_score 계산 시 view_count는 이미 +1 된 값이다.
   */
  @Modifying(clearAutomatically = true)
  @Query(value = """
    UPDATE tb_feed
    SET view_count = view_count + 1,
        hot_score  = LOG10(GREATEST(like_count * 5 + comment_count * 3 + view_count * 0.5, 1))
                     + UNIX_TIMESTAMP(created_at) / :divisor
    WHERE id = :id
    """, nativeQuery = true)
  int incrementViewCount(@Param("id") Long id, @Param("divisor") long divisor);

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

  @Query("SELECT f.product.id, COUNT(f) FROM Feed f WHERE f.product.id IN :productIds AND f.createdAt > :since GROUP BY f.product.id")
  List<Object[]> countByProductIdsAndCreatedAtAfter(@Param("productIds") List<Long> productIds, @Param("since") LocalDateTime since);


  /**
   * 핫 스코어 전체 재계산 (수동 실행 전용).
   * 시간 상수를 바꾼 뒤 저장된 점수를 새 기준으로 맞출 때만 쓴다. 평상시엔 행 단위로 갱신되므로 배치가 없다.
   */
  @Modifying(clearAutomatically = true)
  @Query(value = """
    UPDATE tb_feed
    SET hot_score = LOG10(GREATEST(like_count * 5 + comment_count * 3 + view_count * 0.5, 1))
                    + UNIX_TIMESTAMP(created_at) / :divisor
    WHERE deleted = false
    """, nativeQuery = true)
  int recalculateAllHotScores(@Param("divisor") long divisor);

  /**
   * 최근 N시간 내 생성된 활성 피드 수
   * - 핫 스코어 시간 감쇠 지수 동적 선택에 사용
   */
  @Query("SELECT COUNT(f) FROM Feed f WHERE f.isDeleted = false AND f.createdAt >= :since")
  long countRecentFeeds(@Param("since") LocalDateTime since);

  Long countByHotScoreGreaterThanEqualAndIsDeletedFalse(double hotScoreThreshold);
}