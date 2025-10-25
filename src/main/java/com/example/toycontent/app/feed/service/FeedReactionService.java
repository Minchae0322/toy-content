package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedReactionResponse;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReaction;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedReactionService {

  private final FeedRepository feedRepository;
  private final FeedReactionRepository feedReactionRepository;

  /**
   * 리액션 추가 또는 변경
   * - 기존 리액션이 없으면 추가
   * - 같은 타입 리액션이 있으면 제거 (토글)
   * - 다른 타입 리액션이 있으면 변경
   */
  public FeedReactionResponse.ReactionResult toggleReaction(
      Long feedId, Long userId, FeedReactionType reactionType) {

    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

    // 기존 리액션 조회
    FeedReaction existingReaction = feedReactionRepository
        .findByFeedIdAndUserId(feedId, userId)
        .orElse(null);

    // 1. 기존 리액션이 없는 경우 -> 새로 추가
    if (existingReaction == null) {
      FeedReaction newReaction = feed.addReaction(userId, reactionType);
      feedReactionRepository.save(newReaction);
      return FeedReactionResponse.ReactionResult.added(reactionType);
    }

    // 2. 같은 타입의 리액션인 경우 -> 제거 (토글)
    if (existingReaction.getReactionType() == reactionType) {
      feed.removeReaction(existingReaction);
      feedReactionRepository.delete(existingReaction);
      return FeedReactionResponse.ReactionResult.removed(reactionType);
    }

    // 3. 다른 타입의 리액션인 경우 -> 기존 삭제 후 새로 추가
    feed.removeReaction(existingReaction);
    feedReactionRepository.delete(existingReaction);

    FeedReaction newReaction = feed.addReaction(userId, reactionType);
    feedReactionRepository.save(newReaction);
    return FeedReactionResponse.ReactionResult.changed(reactionType);
  }

  /**
   * 리액션 제거
   */
  public void removeReaction(Long feedId, Long userId) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

    FeedReaction reaction = feedReactionRepository
        .findByFeedIdAndUserId(feedId, userId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.REACTION_NOT_FOUND));

    feed.removeReaction(reaction);
    feedReactionRepository.delete(reaction);
  }

  /**
   * 피드의 리액션 통계 조회
   */
  public FeedReactionResponse.ReactionStats getReactionStats(Long feedId) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

    return FeedReactionResponse.ReactionStats.from(feed.getReactions());
  }

  /**
   * 사용자가 누른 리액션 조회
   */
  public FeedReactionResponse.UserReaction getUserReaction(Long feedId, Long userId) {
    FeedReaction reaction = feedReactionRepository
        .findByFeedIdAndUserId(feedId, userId)
        .orElse(null);

    return FeedReactionResponse.UserReaction.from(reaction);
  }
}
