package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.domain.FeedReaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

  /**
   * 특정 피드에 대한 사용자의 특정 리액션 조회
   */
  Optional<FeedReaction> findByFeedIdAndUserIdAndReactionType(
      Long feedId, Long userId, FeedReactionType reactionType);

  /**
   * 특정 피드에 대한 사용자의 모든 리액션 조회
   */
  @Query("SELECT fr FROM FeedReaction fr WHERE fr.feed.id = :feedId AND fr.userId = :userId")
  Optional<FeedReaction> findByFeedIdAndUserId(
      @Param("feedId") Long feedId,
      @Param("userId") Long userId);

  /**
   * 피드와 사용자, 리액션 타입으로 존재 여부 확인
   */
  boolean existsByFeedIdAndUserIdAndReactionType(
      Long feedId, Long userId, FeedReactionType reactionType);
}
