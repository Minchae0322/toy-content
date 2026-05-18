package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.domain.FeedReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

  /**
   * 특정 피드에 대한 특정 사용자의 활성 리액션 조회 (API 응답용).
   */
  List<FeedReaction> findByFeedIdAndUserIdAndIsActiveTrue(Long feedId, Long userId);

  /**
   * 특정 피드에 대한 특정 사용자의 특정 타입 리액션 조회 (활성/비활성 모두).
   * 토글 시 "처음 좋아요" vs "취소 후 재좋아요"를 구분하기 위해 비활성 행도 함께 조회한다.
   */
  Optional<FeedReaction> findByFeedIdAndUserIdAndReactionType(
      Long feedId, Long userId, FeedReactionType reactionType);

  /**
   * 특정 피드에 대한 특정 사용자의 특정 타입 활성 리액션 존재 여부
   */
  boolean existsByFeedIdAndUserIdAndReactionTypeAndIsActiveTrue(
      Long feedId, Long userId, FeedReactionType reactionType);

  /**
   * 특정 피드의 특정 타입 활성 리액션 개수
   */
  long countByFeedIdAndReactionTypeAndIsActiveTrue(Long feedId, FeedReactionType reactionType);

  /**
   * 여러 피드에 대한 특정 사용자의 활성 리액션 일괄 조회 (목록용)
   */
  @Query("SELECT fr FROM FeedReaction fr " +
      "WHERE fr.feed.id IN :feedIds " +
      "AND fr.userId = :userId " +
      "AND fr.isActive = true")
  List<FeedReaction> findByFeedIdsAndUserId(
      @Param("feedIds") List<Long> feedIds,
      @Param("userId") Long userId
  );

}
