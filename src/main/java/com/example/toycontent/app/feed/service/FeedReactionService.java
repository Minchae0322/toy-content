package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedReactionResponse;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReaction;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.external.user.dto.ExternalAttachmentFileDto;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
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
  private final NotificationService notificationService;
  private final ExternalUserInfoService externalUserInfoService;
  private final ExpGrantService expGrantService;

  /**
   * 특정 타입 리액션 토글 (좋아요 또는 핫 개별 토글)
   *
   * 피드에 비관적 락을 걸고, 해당 유저의 동일 타입 리액션이 존재하면 제거하고 없으면 추가한다.
   */
  @Transactional
  public FeedReactionResponse.ReactionResult toggleReaction(
      Long feedId, Long actionUserId, FeedReactionType reactionType) {

    Feed feed = findFeedWithLockOrElseThrow(feedId);

    return feedReactionRepository
        .findByFeedIdAndUserIdAndReactionType(feedId, actionUserId, reactionType)
        .map(existing -> removeReactionAndSave(feed, existing, reactionType))
        .orElseGet(() -> addReactionAndSave(feed, actionUserId, reactionType));
  }

  /**
   * 리액션 추가
   *
   * 피드에 리액션을 등록하고, 피드 작성자에게 좋아요 알림을 발송한다.
   * 알림에는 리액션을 누른 유저의 닉네임과 프로필 이미지가 포함된다.
   */
  private FeedReactionResponse.ReactionResult addReactionAndSave(
      Feed feed, Long actionUserId, FeedReactionType type) {

    FeedReaction reaction = feed.addReaction(actionUserId, type);
    feedReactionRepository.save(reaction);
    feedRepository.save(feed);

    ExternalUserInfo externalUserInfo = externalUserInfoService.getUserInfo(actionUserId);

    notificationService.notifyFeedLike(
            feed.getUserId(),
            actionUserId,
            externalUserInfo.getNickname(),
            Optional.ofNullable(externalUserInfo.getProfileImageFile())
                    .map(ExternalAttachmentFileDto::getFileUrl)
                    .orElse(null),
            feed.getId(),
            feed.getProductNameCustom()
    );

    // 피드 작성자에게 리액션 수신 EXP 지급 (본인 리액션 제외)
    if (!actionUserId.equals(feed.getUserId())) {
      expGrantService.grantFeedReaction(feed.getUserId(), feed.getId());
    }

    return FeedReactionResponse.ReactionResult.added(type, feed.getLikeCount());
  }

  /**
   * 리액션 제거
   */
  private FeedReactionResponse.ReactionResult removeReactionAndSave(
      Feed feed, FeedReaction reaction, FeedReactionType type) {

    feed.removeReaction(reaction);
    feedReactionRepository.delete(reaction);
    feedRepository.save(feed);

    return FeedReactionResponse.ReactionResult.removed(type, feed.getLikeCount());
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