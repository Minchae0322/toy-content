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
   * 피드에 비관적 락을 걸고, 다음 3가지 케이스로 분기한다.
   * <ul>
   *   <li>활성 리액션 존재 → 비활성화 (좋아요 취소)</li>
   *   <li>비활성 리액션 존재 → 재활성화. 푸시 미발송 (과거에 이미 발송).</li>
   *   <li>리액션 없음 → 새로 생성 + 피드 작성자에게 푸시 발송.</li>
   * </ul>
   * 푸시는 사용자별로 같은 피드에 대해 "처음 좋아요" 시 1회만 발송된다.
   */
  @Transactional
  public FeedReactionResponse.ReactionResult toggleReaction(
      Long feedId, Long actionUserId, FeedReactionType reactionType) {

    Feed feed = findFeedWithLockOrElseThrow(feedId);

    Optional<FeedReaction> existing = feedReactionRepository
        .findByFeedIdAndUserIdAndReactionType(feedId, actionUserId, reactionType);

    if (existing.isPresent()) {
      FeedReaction reaction = existing.get();
      return reaction.isActive()
          ? cancelReaction(feed, reaction, reactionType)
          : restoreReactionSilently(feed, reaction, reactionType);
    }

    return addNewReactionWithPush(feed, actionUserId, reactionType);
  }

  /**
   * 첫 리액션 생성 — 행을 새로 만들고 작성자에게 푸시 알림 발송.
   */
  private FeedReactionResponse.ReactionResult addNewReactionWithPush(
      Feed feed, Long actionUserId, FeedReactionType type) {

    FeedReaction reaction = feed.addReaction(actionUserId, type);
    feedReactionRepository.save(reaction);
    feedRepository.save(feed);

    boolean isSelfReaction = actionUserId.equals(feed.getUserId());
    if (!isSelfReaction) {
      sendReactionNotification(feed, actionUserId);
      expGrantService.grantFeedReaction(feed.getUserId(), feed.getId());
    }

    return FeedReactionResponse.ReactionResult.added(type, feed.getLikeCount());
  }

  /**
   * 활성 리액션 비활성화 (취소). 행은 유지하여 재리액션 시 푸시 중복 방지에 사용.
   */
  private FeedReactionResponse.ReactionResult cancelReaction(
      Feed feed, FeedReaction reaction, FeedReactionType type) {

    feed.deactivateReaction(reaction);
    feedRepository.save(feed);

    return FeedReactionResponse.ReactionResult.removed(type, feed.getLikeCount());
  }

  /**
   * 비활성 리액션 재활성화. 푸시는 보내지 않음 (이미 과거에 발송됨).
   */
  private FeedReactionResponse.ReactionResult restoreReactionSilently(
      Feed feed, FeedReaction reaction, FeedReactionType type) {

    feed.reactivateReaction(reaction);
    feedRepository.save(feed);

    // EXP는 좋아요를 받을 때마다 적립되는 동작 유지
    if (!reaction.getUserId().equals(feed.getUserId())) {
      expGrantService.grantFeedReaction(feed.getUserId(), feed.getId());
    }

    return FeedReactionResponse.ReactionResult.added(type, feed.getLikeCount());
  }

  private void sendReactionNotification(Feed feed, Long actionUserId) {
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
  }

  /**
   * 특정 타입 리액션 명시적 제거 (DELETE 엔드포인트). 활성 상태가 아니면 예외.
   */
  public void removeReaction(Long feedId, Long userId, FeedReactionType reactionType) {
    Feed feed = findFeedWithLockOrElseThrow(feedId);

    FeedReaction reaction = feedReactionRepository
        .findByFeedIdAndUserIdAndReactionType(feedId, userId, reactionType)
        .filter(FeedReaction::isActive)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.REACTION_NOT_FOUND));

    feed.deactivateReaction(reaction);
    feedRepository.save(feed);
  }


  /**
   * 사용자가 누른 활성 리액션 조회 (좋아요, 핫 모두)
   */
  public FeedReactionResponse.UserReactions getUserReactions(Long feedId, Long userId) {
    List<FeedReaction> reactions = feedReactionRepository
        .findByFeedIdAndUserIdAndIsActiveTrue(feedId, userId);

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
