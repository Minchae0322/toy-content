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
   *   engagementScore = (like_count × 2) + (view_count × 0.1)
   *   decayFactor     = POWER(GREATEST(경과시간(h) + 2, 1), 1.5)
   * </pre>
   *
   * <p>계산 근거:</p>
   * <ul>
   *   <li>좋아요(×2): 적극적인 참여 행동으로 가중치를 높게 부여</li>
   *   <li>조회수(×0.1): 수동적 행동이므로 낮은 가중치 부여</li>
   *   <li>시간 감쇠(1.5승): 시간이 지날수록 스코어가 급격히 하락하여 최신 콘텐츠 우선 노출</li>
   *   <li>+2 보정: 생성 직후(0시간)에도 0으로 나누는 것을 방지</li>
   * </ul>
   *
   * @param since 이 시각 이후 updated_at이 갱신된 피드만 대상
   * @return 업데이트된 피드 수
   */
  @Modifying(clearAutomatically = true)
  @Query(value = """
    UPDATE tb_feed
    SET hot_score = (like_count * 2 + view_count * 0.1)
                    / POWER(GREATEST(TIMESTAMPDIFF(HOUR, created_at, NOW()) + 2, 1), 1.5)
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
    SET hot_score = (like_count * 2 + view_count * 0.1)
                    / POWER(GREATEST(TIMESTAMPDIFF(HOUR, created_at, NOW()) + 2, 1), 1.5)
    WHERE deleted = false
      AND created_at >= DATE_SUB(NOW(), INTERVAL :recentDays DAY)
    """, nativeQuery = true)
  int bulkUpdateHotScoreAll(@Param("recentDays") int recentDays);

  Long countByHotScoreGreaterThanEqualAndIsDeletedFalse(double hotScoreThreshold);
}