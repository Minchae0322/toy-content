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
   * 특정 피드에 대한 특정 사용자의 모든 리액션 조회
   */
  List<FeedReaction> findByFeedIdAndUserId(Long feedId, Long userId);

  /**
   * 특정 피드에 대한 특정 사용자의 특정 타입 리액션 조회
   */
  Optional<FeedReaction> findByFeedIdAndUserIdAndReactionType(
      Long feedId, Long userId, FeedReactionType reactionType);

  /**
   * 특정 피드에 대한 특정 사용자의 특정 타입 리액션 존재 여부
   */
  boolean existsByFeedIdAndUserIdAndReactionType(
      Long feedId, Long userId, FeedReactionType reactionType);

  /**
   * 특정 피드의 특정 타입 리액션 개수
   */
  long countByFeedIdAndReactionType(Long feedId, FeedReactionType reactionType);

  /**
   * 여러 피드에 대한 특정 사용자의 리액션 일괄 조회 (목록용)
   */
  @Query("SELECT fr FROM FeedReaction fr " +
      "WHERE fr.feed.id IN :feedIds " +
      "AND fr.userId = :userId")
  List<FeedReaction> findByFeedIdsAndUserId(
      @Param("feedIds") List<Long> feedIds,
      @Param("userId") Long userId
  );

}
