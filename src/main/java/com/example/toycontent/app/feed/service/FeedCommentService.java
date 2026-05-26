package com.example.toycontent.app.feed.service;


import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest.CommentCreate;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest.CommentUpdate;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedComment;
import com.example.toycontent.app.feed.repository.FeedCommentRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantInfo;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantResult;
import com.example.toycontent.external.user.dto.ExternalAttachmentFileDto;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedCommentService {

  private final FeedRepository feedRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final ExternalUserInfoService externalUserInfoService;

  private final NotificationService notificationService;
  private final ExpGrantService expGrantService;

  @Transactional
  public Page<CommentItem> getComments(Long feedId, Pageable pageable) {
    return feedCommentRepository.findVisibleCommentsWithReplies(feedId, pageable);
  }

  /**
   * 피드에 댓글 또는 답글을 생성한다.
   */
  @Transactional
  public FeedCommentResponse.Created createComment(Long feedId, CommentCreate request, Long creatorId) {
    // 피드 존재 확인
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

    // 답글인 경우 부모 댓글 유효성 검증 (루트 댓글이면 null 반환)
    FeedComment parent = resolveParent(feedId, request.getParentCommentId());

    // 댓글 엔티티 생성 및 저장, 피드의 총 댓글 수 증가 (답글도 포함)
    ExternalUserInfo externalUserInfo = externalUserInfoService.getUserInfo(creatorId);

    FeedComment comment = toFeedComment(feed, request, creatorId, externalUserInfo, parent);
    feedCommentRepository.save(comment);
    feed.incrementCommentCount();

    // 알림 대상:
    //  - 답글: 부모 댓글 작성자 + 같은 부모에 답글 단 사람들 (중복/본인 제외)
    //  - 일반 댓글: 피드 작성자
    List<Long> notifyTargetUserIds = resolveCommentNotifyTargets(feed, parent, creatorId);

    String actorProfileUrl = Optional.ofNullable(externalUserInfo.getProfileImageFile())
        .map(ExternalAttachmentFileDto::getFileUrl)
        .orElse(null);

    for (Long targetUserId : notifyTargetUserIds) {
      notificationService.notifyFeedComment(
          targetUserId,
          creatorId,
          externalUserInfo.getNickname(),
          actorProfileUrl,
          feedId,
          feed.getProductNameCustom()
      );
    }

    // 댓글 작성에 대한 EXP 지급
    ExpGrantResult grant = expGrantService.grantCommentCreate(creatorId, comment.getId());

    return FeedCommentResponse.Created.of(comment, ExpGrantInfo.aggregate(grant));
  }

  /**
   * 답글 생성 시 부모 댓글의 유효성을 검증하고 반환한다.
   *
   * <p>검증 항목:
   * <ul>
   *   <li>{@code parentCommentId}가 null이면 루트 댓글 생성으로 간주하고 null 반환</li>
   *   <li>부모 댓글이 피드에 실제로 존재해야 함</li>
   *   <li>부모 댓글이 이미 답글이면 안 됨 (1뎁스 제한 — 답글의 답글 차단)</li>
   *   <li>부모 댓글이 삭제된 상태면 안 됨 — 삭제된 댓글은 "삭제된 댓글입니다"로 마스킹돼
   *       목록에만 노출되는 플레이스홀더이므로 새 답글은 달 수 없음</li>
   * </ul>
   */
  private FeedComment resolveParent(Long feedId, Long parentCommentId) {
    if (parentCommentId == null) {
      return null;
    }

    FeedComment parent = findFeedCommentByIdAndFeedIdOrElseThrow(parentCommentId, feedId);

    if (parent.isReply()) {
      throw new RestApiException(FeedErrorCode.REPLY_DEPTH_EXCEEDED);
    }

    if (Boolean.TRUE.equals(parent.getDeleted())) {
      throw new RestApiException(FeedErrorCode.PARENT_COMMENT_DELETED);
    }

    return parent;
  }

  private List<Long> resolveCommentNotifyTargets(Feed feed, FeedComment parent, Long creatorId) {
    if (parent == null) {
      return List.of(feed.getUserId());
    }

    Set<Long> targets = new LinkedHashSet<>();
    targets.add(parent.getCreatorId());
    targets.addAll(feedCommentRepository.findReplyCreatorIdsByParentId(parent.getId()));
    targets.remove(creatorId);
    return List.copyOf(targets);
  }

  private FeedComment toFeedComment(Feed feed, CommentCreate create, Long creatorId,
      ExternalUserInfo externalUserInfo, FeedComment parent) {
    return FeedComment.builder()
        .content(create.getContent())
        .creatorId(creatorId)
        .creatorNickname(externalUserInfo.getNickname())
        .creatorProfileUrl(externalUserInfo.getProfileImageFile() != null
            ? externalUserInfo.getProfileImageFile().getFileUrl() : null)
        .feed(feed)
        .parent(parent)
        .deleted(false)
        .build();
  }

  @Transactional
  public FeedCommentResponse.Updated updateComment(Long feedId, Long commentId, CommentUpdate request) {
    FeedComment comment = findFeedCommentByIdAndFeedIdOrElseThrow(commentId, feedId);

    comment.updateContent(request.getContent());

    return FeedCommentResponse.Updated.of(comment);
  }

  @Transactional
  public void deleteComment(Long feedId, Long commentId) {
    FeedComment comment = findFeedCommentByIdAndFeedIdOrElseThrow(commentId, feedId);

    comment.getFeed().decrementCommentCount();

    comment.delete();
  }

  private FeedComment findFeedCommentByIdAndFeedIdOrElseThrow(Long commentId, Long feedId) {
    return feedCommentRepository.findByIdAndFeedId(commentId, feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_COMMENT_NOT_FOUND));
  }


}
