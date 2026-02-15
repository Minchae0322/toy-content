package com.example.toycontent.app.battle.service;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentRequest;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleItemComment;
import com.example.toycontent.app.battle.domain.BattleItemCommentLike;
import com.example.toycontent.app.battle.repository.BattleItemCommentLikeRepository;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleItemCommentService {

  private final BattleItemRepository battleItemRepository;
  private final BattleItemCommentRepository commentRepository;
  private final BattleItemCommentLikeRepository likeRepository;

  @Transactional
  public void createComment(Long battleId, Long itemId, Long userId, BattleItemCommentRequest.Create request) {
    BattleItem battleItem = battleItemRepository.findById(itemId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

    BattleItemComment comment = BattleItemComment.builder()
        .battleItem(battleItem)
        .creatorId(userId)
        .content(request.getContent())
        .build();

    commentRepository.save(comment);
  }

  @Transactional(readOnly = true)
  public Slice<BattleItemCommentResponse.Detail> getComments(
      Long battleId, Long itemId, Long userId, Pageable pageable) {
    return commentRepository.findComments(itemId, userId, pageable);
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