package com.example.toycontent.app.battle.service;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentRequest;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleItemComment;
import com.example.toycontent.app.battle.domain.BattleItemCommentLike;
import com.example.toycontent.app.battle.repository.BattleItemCommentLikeRepository;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import java.util.Optional;
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
        .orElseThrow(() -> new IllegalArgumentException("배틀 아이템을 찾을 수 없습니다."));

    BattleItemComment comment = BattleItemComment.builder()
        .battleItem(battleItem)
        .creatorId(userId)
        .content(request.getContent())
        .build();

    commentRepository.save(comment);
  }

  public Slice<BattleItemCommentResponse.Detail> getComments(
      Long battleId, Long itemId, Long userId, String sort, Pageable pageable) {

    Slice<BattleItemComment> comments = "likes".equals(sort)
        ? commentRepository.findByBattleItemIdAndIsDeletedFalseOrderByLikeCountDesc(itemId, pageable)
        : commentRepository.findByBattleItemIdAndIsDeletedFalseOrderByCreatedAtDesc(itemId, pageable);

    return comments.map(comment -> {
      boolean isLiked = likeRepository.existsByBattleItemCommentIdAndMemberId(comment.getId(), userId);
      // TODO: memberId로 닉네임 조회 (회원 서비스 연동)
      String nickname = "사용자";
      return BattleItemCommentResponse.Detail.of(comment, nickname, isLiked, userId);
    });
  }

  @Transactional
  public void updateComment(Long commentId, Long userId, BattleItemCommentRequest.Update request) {
    BattleItemComment comment = findCommentById(commentId);
    validateWriter(comment, userId);
    comment.updateContent(request.getContent());
  }

  @Transactional
  public void deleteComment(Long commentId, Long userId) {
    BattleItemComment comment = findCommentById(commentId);
    validateWriter(comment, userId);
    comment.softDelete();
  }

  @Transactional
  public BattleItemCommentResponse.LikeResult toggleLike(Long commentId, Long userId) {
    BattleItemComment comment = findCommentById(commentId);

    Optional<BattleItemCommentLike> existing =
        likeRepository.findByBattleItemCommentIdAndMemberId(commentId, userId);

    boolean isLiked;
    if (existing.isPresent()) {
      likeRepository.delete(existing.get());
      comment.decrementLikeCount();
      isLiked = false;
    } else {
      BattleItemCommentLike like = BattleItemCommentLike.builder()
          .battleItemComment(comment)
          .creatorId(userId)
          .build();
      likeRepository.save(like);
      comment.incrementLikeCount();
      isLiked = true;
    }

    return BattleItemCommentResponse.LikeResult.builder()
        .isLiked(isLiked)
        .likeCount(comment.getLikeCount())
        .build();
  }

  private BattleItemComment findCommentById(Long commentId) {
    return commentRepository.findById(commentId)
        .filter(BattleItemComment::isActive)
        .orElseThrow(() -> new IllegalArgumentException("변론을 찾을 수 없습니다."));
  }

  private void validateWriter(BattleItemComment comment, Long userId) {
    if (!comment.isWrittenBy(userId)) {
      throw new IllegalArgumentException("본인이 작성한 변론만 수정/삭제할 수 있습니다.");
    }
  }
}