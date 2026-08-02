package com.example.toycontent.app.battle.service;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentRequest;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleItemComment;
import com.example.toycontent.app.battle.domain.BattleItemCommentLike;
import com.example.toycontent.app.battle.repository.BattleItemCommentLikeRepository;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.external.user.dto.ExternalAttachmentFileDto;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BattleItemCommentService {

  private final BattleItemRepository battleItemRepository;
  private final BattleItemCommentRepository commentRepository;
  private final BattleItemCommentLikeRepository likeRepository;
  private final ExternalUserInfoService externalUserInfoService;
  private final NotificationService notificationService;

  @Transactional
  public void createComment(Long battleId, Long itemId, Long actionUserId, BattleItemCommentRequest.Create request) {
    log.info("[battle] 댓글 작성 시작 - battleId: {}, itemId: {}, userId: {}", battleId, itemId, actionUserId);

    try {
      BattleItem battleItem = battleItemRepository.findById(itemId)
              .orElseThrow(() -> {
                log.warn("[battle] 배틀 아이템 조회 실패 - itemId: {}", itemId);
                return new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND);
              });
      log.debug("[battle] 1단계 통과 - 배틀 아이템 조회 완료");

      ExternalUserInfo userInfo = externalUserInfoService.getUserInfo(actionUserId);
      log.debug("[battle] 2단계 통과 - 유저 정보 조회 완료: nickname={}", userInfo.getNickname());

      BattleItemComment comment = BattleItemComment.builder()
              .battleItem(battleItem)
              .creatorId(actionUserId)
              .creatorNickname(userInfo.getNickname())
              .creatorProfileImageUrl(Optional.ofNullable(userInfo.getProfileImageFile())
                      .map(ExternalAttachmentFileDto::getFileUrl)
                      .orElse(null))
              .content(request.getContent())
              .build();

      log.debug("[battle] 3단계 통과 - 댓글 엔티티 생성 완료");

      battleItem.getBattle().incrementTotalCommentCount();
      log.debug("[battle] 4단계 통과 - 댓글 수 증가 완료");

      commentRepository.save(comment);
      log.debug("[battle] 5단계 통과 - 댓글 저장 완료");

      // 알림은 이벤트로 등록만 한다. 실제 발행은 트랜잭션 커밋 이후에 일어나므로,
      // 이 트랜잭션이 롤백되면 알림도 함께 폐기된다(유령 알림 방지).
      notificationService.notifyBattleItemComment(
              battleItem.getRegisterId(),
              actionUserId,
              userInfo.getNickname(),
              Optional.ofNullable(userInfo.getProfileImageFile())
                      .map(ExternalAttachmentFileDto::getFileUrl)
                      .orElse(null),
              battleItem.getBattle().getId(),
              battleItem.getBattle().getTitle(),
              battleItem.getId(),
              battleItem.getDisplayName()
      );

      log.info("[battle] 댓글 작성 완료 - commentId: {}, battleId: {}, itemId: {}", comment.getId(), battleId, itemId);

    } catch (RestApiException e) {
      throw e;
    } catch (Exception e) {
      log.error("[battle] 댓글 작성 실패 - battleId: {}, itemId: {}, userId: {}, 원인: {}",
              battleId, itemId, actionUserId, e.getMessage(), e);
      throw e;
    }
  }

  @Transactional(readOnly = true)
  public Slice<BattleItemCommentResponse.Detail> getComments(
      Long battleId, Long itemId, Long userId, Pageable pageable) {
    return commentRepository.findBattleItemComments(itemId, userId, pageable);
  }

  @Transactional(readOnly = true)
  public Slice<BattleItemCommentResponse.Detail> getBattleComments(
      Long battleId, Long userId, Pageable pageable) {
    return commentRepository.findBattleComments(battleId, userId, pageable);
  }

  @Transactional
  public void updateComment(Long commentId, Long userId, BattleItemCommentRequest.Update request) {
    BattleItemComment comment = getCommentById(commentId);
    validateWriter(comment, userId);
    comment.updateContent(request.getContent());
  }

  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    BattleItemComment comment = getCommentById(commentId);
    validateWriter(comment, userId);

    Battle battle = comment.getBattleItem().getBattle();
    battle.decrementTotalCommentCount();

    comment.softDelete();
  }

  @Transactional
  public BattleItemCommentResponse.LikeResult toggleLike(Long commentId, Long userId) {
    BattleItemComment comment = getCommentById(commentId);

    boolean isLiked = likeRepository.findByBattleItemCommentIdAndCreatorId(commentId, userId)
        .map(like -> {
          likeRepository.delete(like);
          comment.decrementLikeCount();
          return false;
        })
        .orElseGet(() -> {
          likeRepository.save(BattleItemCommentLike.builder()
              .battleItemComment(comment)
              .creatorId(userId)
              .build());
          comment.incrementLikeCount();
          return true;
        });


    return BattleItemCommentResponse.LikeResult.of(isLiked, comment.getLikeCount());
  }

  private BattleItemComment getCommentById(Long commentId) {
    return commentRepository.findById(commentId)
        .filter(BattleItemComment::isActive)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));
  }

  private void validateWriter(BattleItemComment comment, Long userId) {
    if (!comment.isWrittenBy(userId)) {
      throw new RestApiException(BattleErrorCode.NOT_COMMENT_WRITER);
    }
  }
}