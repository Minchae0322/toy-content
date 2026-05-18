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

  @Query("SELECT f.product.id, COUNT(f) FROM Feed f WHERE f.product.id IN :productIds AND f.createdAt > :since GROUP BY f.product.id")
  List<Object[]> countByProductIdsAndCreatedAtAfter(@Param("productIds") List<Long> productIds, @Param("since") LocalDateTime since);

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


  /**
   * 피드 핫 스코어 벌크 업데이트 (최근 활동 대상)
   *
   * 매시간 실행되며, 최근 일정 시간 내 활동(좋아요, 조회 등)이 있어
   * updated_at이 갱신된 피드만 대상으로 핫 스코어를 재계산한다.
   *
   * <p>핫 스코어 계산식:</p>
   * <pre>
   *   hotScore = engagementScore / decayFactor
   *
   *   engagementScore = (like_count × 5) + (comment_count × 3) + (view_count × 0.5)
   *   decayFactor     = POWER(GREATEST(경과시간(h) + 24, 1), 0.9)
   * </pre>
   *
   * <p>계산 근거 (저트래픽 단계 기준):</p>
   * <ul>
   *   <li>좋아요(×5) > 댓글(×3) > 조회(×0.5): 참여 강도 순. 1 like = 10 view,
   *       1 comment = 6 view 가중치. 누적 조회량이 많아질수록 view도 의미 있는 시그널이 됨.</li>
   *   <li>시간 감쇠(0.9승, +24 평탄): 3일 정도 누적 호응을 받은 피드는 7일차에서도
   *       경쟁력을 갖되, 그 이상은 새 시그널 없이 무한 노출되지 않도록 균형점 잡음.</li>
   * </ul>
   *
   * @param since 이 시각 이후 updated_at이 갱신된 피드만 대상
   * @return 업데이트된 피드 수
   */
  @Modifying(clearAutomatically = true)
  @Query(value = """
    UPDATE tb_feed
    SET hot_score = (like_count * 5 + comment_count * 3 + view_count * 0.5)
                    / POWER(GREATEST(TIMESTAMPDIFF(HOUR, created_at, NOW()) + 24, 1), 0.9)
    WHERE deleted = false
      AND updated_at >= :since
    """, nativeQuery = true)
  int bulkUpdateHotScoreRecent(@Param("since") LocalDateTime since);

  /**
   * 피드 핫 스코어 벌크 업데이트 (전체 재계산)
   *
   * 새벽 시간대에 실행되며, 최근 N일 이내 생성된 모든 활성 피드를 대상으로
   * 시간 감쇠를 반영하여 핫 스코어를 재계산한다.
   *
   * @param recentDays 재계산 대상 기간 (일)
   * @return 업데이트된 피드 수
   * @see #bulkUpdateHotScoreRecent(LocalDateTime) 핫 스코어 계산식 상세
   */
  @Modifying(clearAutomatically = true)
  @Query(value = """
    UPDATE tb_feed
    SET hot_score = (like_count * 5 + comment_count * 3 + view_count * 0.5)
                    / POWER(GREATEST(TIMESTAMPDIFF(HOUR, created_at, NOW()) + 24, 1), 0.9)
    WHERE deleted = false
      AND created_at >= DATE_SUB(NOW(), INTERVAL :recentDays DAY)
    """, nativeQuery = true)
  int bulkUpdateHotScoreAll(@Param("recentDays") int recentDays);

  Long countByHotScoreGreaterThanEqualAndIsDeletedFalse(double hotScoreThreshold);
}