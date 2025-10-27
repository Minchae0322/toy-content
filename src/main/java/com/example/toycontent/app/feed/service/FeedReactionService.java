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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedReactionService {

  private final FeedRepository feedRepository;
  private final FeedReactionRepository feedReactionRepository;

  /**
   * 특정 타입 리액션 토글 (좋아요 또는 핫 개별 토글)
   */
  public FeedReactionResponse.ReactionResult toggleReaction(
      Long feedId, Long userId, FeedReactionType reactionType) {

    Feed feed = findFeedWithLockOrElseThrow(feedId);

    return feedReactionRepository
        .findByFeedIdAndUserIdAndReactionType(feedId, userId, reactionType)
        .map(existing -> removeReactionAndSave(feed, existing, reactionType))
        .orElseGet(() -> addReactionAndSave(feed, userId, reactionType));
  }

  /**
   * 리액션 추가
   */
  private FeedReactionResponse.ReactionResult addReactionAndSave(
      Feed feed, Long userId, FeedReactionType type) {

    FeedReaction reaction = feed.addReaction(userId, type);
    feedReactionRepository.save(reaction);
    feedRepository.save(feed);

    return FeedReactionResponse.ReactionResult.added(type, feed.getLikeCount(), feed.getHotCount());
  }

  /**
   * 리액션 제거
   */
  private FeedReactionResponse.ReactionResult removeReactionAndSave(
      Feed feed, FeedReaction reaction, FeedReactionType type) {

    feed.removeReaction(reaction);
    feedReactionRepository.delete(reaction);
    feedRepository.save(feed);

    return FeedReactionResponse.ReactionResult.removed(type, feed.getLikeCount(), feed.getHotCount());
  }

  /**
   * 특정 타입 리액션 제거
   */
  public void removeReaction(Long feedId, Long userId, FeedReactionType reactionType) {
    Feed feed = findFeedWithLockOrElseThrow(feedId);

    FeedReaction reaction = feedReactionRepository
        .findByFeedIdAndUserIdAndReactionType(feedId, userId, reactionType)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.REACTION_NOT_FOUND));

    feed.removeReaction(reaction);
    feedReactionRepository.delete(reaction);
    feedRepository.save(feed);
  }


  /**
   * 사용자가 누른 리액션 조회 (좋아요, 핫 모두)
   */
  public FeedReactionResponse.UserReactions getUserReactions(Long feedId, Long userId) {
    List<FeedReaction> reactions = feedReactionRepository
        .findByFeedIdAndUserId(feedId, userId);

    Set<FeedReactionType> reactionTypes = reactions.stream()
        .map(FeedReaction::getReactionType)
        .collect(Collectors.toSet());

    return FeedReactionResponse.UserReactions.builder()
        .hasLike(reactionTypes.contains(FeedReactionType.LIKE))
        .hasHot(reactionTypes.contains(FeedReactionType.HOT))
        .build();
  }

  private Feed findFeedWithLockOrElseThrow(Long feedId) {
    return feedRepository.findByIdWithPessimisticLock(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));
  }
}