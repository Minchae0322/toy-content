package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.BattleFixture;
import com.example.toycontent.support.fixture.BattleItemFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleItemCommentService")
class BattleItemCommentServiceTest {

  private static final long BATTLE_ID = 1L;
  private static final long ITEM_ID = 10L;
  private static final long COMMENT_ID = 100L;
  private static final long WRITER_ID = 200L;
  private static final long OTHER_USER_ID = 300L;
  private static final String NICKNAME = "댓글유저";

  @Mock private BattleItemRepository battleItemRepository;
  @Mock private BattleItemCommentRepository commentRepository;
  @Mock private BattleItemCommentLikeRepository likeRepository;
  @Mock private ExternalUserInfoService externalUserInfoService;
  @Mock private NotificationService notificationService;

  @InjectMocks private BattleItemCommentService service;

  @Nested
  @DisplayName("createComment - 댓글 생성")
  class CreateComment {

    @Test
    @DisplayName("댓글 생성 시 배틀의 totalCommentCount가 1 증가한다")
    void 댓글수_증가() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItemFixture.custom(battle, "아이템");
      int previousCount = battle.getTotalCommentCount();
      given(battleItemRepository.findById(ITEM_ID)).willReturn(Optional.of(item));
      given(externalUserInfoService.getUserInfo(WRITER_ID))
          .willReturn(externalUser());

      // when
      service.createComment(BATTLE_ID, ITEM_ID, WRITER_ID, createRequest("좋은 변론"));

      // then
      assertThat(battle.getTotalCommentCount()).isEqualTo(previousCount + 1);
    }

    @Test
    @DisplayName("댓글 생성 시 아이템 등록자에게 알림이 발송된다")
    void 알림_발송() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItemFixture.custom(battle, "아이템");
      given(battleItemRepository.findById(ITEM_ID)).willReturn(Optional.of(item));
      given(externalUserInfoService.getUserInfo(WRITER_ID))
          .willReturn(externalUser());

      // when
      service.createComment(BATTLE_ID, ITEM_ID, WRITER_ID, createRequest("좋은 변론"));

      // then
      then(notificationService).should().notifyBattleItemComment(
          any(), any(), any(), any(), any(), any(), any(), any()
      );
    }

    @Test
    @DisplayName("존재하지 않는 아이템에 댓글 작성 시 예외를 던진다")
    void 아이템_없음_예외() {
      // given
      given(battleItemRepository.findById(ITEM_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          service.createComment(BATTLE_ID, ITEM_ID, WRITER_ID, createRequest("변론")))
          .isInstanceOf(RestApiException.class);

      then(notificationService).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("updateComment - 댓글 수정")
  class UpdateComment {

    @Test
    @DisplayName("작성자가 수정하면 내용이 반영된다")
    void 작성자_수정_성공() {
      // given
      BattleItemComment comment = commentBy(WRITER_ID, "원본 변론");
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

      // when
      service.updateComment(COMMENT_ID, WRITER_ID, updateRequest("수정된 변론"));

      // then
      assertThat(comment.getContent()).isEqualTo("수정된 변론");
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 수정하면 예외를 던진다")
    void 타인_수정_예외() {
      // given
      BattleItemComment comment = commentBy(WRITER_ID, "원본 변론");
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

      // when & then
      assertThatThrownBy(() ->
          service.updateComment(COMMENT_ID, OTHER_USER_ID, updateRequest("변경 시도")))
          .isInstanceOf(RestApiException.class);

      assertThat(comment.getContent())
          .as("예외 시 내용 불변").isEqualTo("원본 변론");
    }

    @Test
    @DisplayName("이미 삭제된 댓글은 수정 시 예외를 던진다 (isActive false)")
    void 삭제된_댓글_수정_예외() {
      // given
      BattleItemComment comment = commentBy(WRITER_ID, "원본");
      comment.softDelete();
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

      // when & then
      assertThatThrownBy(() ->
          service.updateComment(COMMENT_ID, WRITER_ID, updateRequest("...")))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("deleteComment - 댓글 삭제")
  class DeleteComment {

    @Test
    @DisplayName("작성자가 삭제하면 논리삭제되고 배틀의 댓글 수가 감소한다")
    void 정상_삭제() {
      // given
      Battle battle = BattleFixture.active();
      battle.incrementTotalCommentCount();
      int previousCount = battle.getTotalCommentCount();
      BattleItem item = BattleItemFixture.custom(battle, "아이템");
      BattleItemComment comment = BattleItemComment.builder()
          .id(COMMENT_ID).battleItem(item).creatorId(WRITER_ID)
          .creatorNickname(NICKNAME).content("변론").isDeleted(false)
          .build();
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

      // when
      service.deleteComment(COMMENT_ID, WRITER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(comment.getIsDeleted()).as("논리삭제 플래그").isTrue();
        softly.assertThat(battle.getTotalCommentCount())
            .as("배틀 댓글 수 1 감소").isEqualTo(previousCount - 1);
      });
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 삭제하면 예외를 던진다")
    void 타인_삭제_예외() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItemFixture.custom(battle, "아이템");
      BattleItemComment comment = BattleItemComment.builder()
          .id(COMMENT_ID).battleItem(item).creatorId(WRITER_ID)
          .creatorNickname(NICKNAME).content("변론").isDeleted(false)
          .build();
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));

      // when & then
      assertThatThrownBy(() -> service.deleteComment(COMMENT_ID, OTHER_USER_ID))
          .isInstanceOf(RestApiException.class);

      assertThat(comment.getIsDeleted()).isFalse();
    }
  }

  @Nested
  @DisplayName("toggleLike - 공감 토글")
  class ToggleLike {

    @Test
    @DisplayName("기존 공감이 없으면 공감이 생성되고 likeCount가 증가한다")
    void 신규_공감() {
      // given
      BattleItemComment comment = commentBy(WRITER_ID, "변론");
      int previousLike = comment.getLikeCount();
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
      given(likeRepository.findByBattleItemCommentIdAndCreatorId(COMMENT_ID, OTHER_USER_ID))
          .willReturn(Optional.empty());

      // when
      BattleItemCommentResponse.LikeResult result = service.toggleLike(COMMENT_ID, OTHER_USER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getIsLiked()).as("공감 추가됨").isTrue();
        softly.assertThat(result.getLikeCount()).isEqualTo(previousLike + 1);
        softly.assertThat(comment.getLikeCount()).isEqualTo(previousLike + 1);
      });
      then(likeRepository).should().save(any(BattleItemCommentLike.class));
      then(likeRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("기존 공감이 있으면 삭제되고 likeCount가 감소한다")
    void 공감_취소() {
      // given
      BattleItemComment comment = commentBy(WRITER_ID, "변론");
      comment.incrementLikeCount();
      comment.incrementLikeCount();
      int previousLike = comment.getLikeCount();
      BattleItemCommentLike existing = BattleItemCommentLike.builder()
          .battleItemComment(comment).creatorId(OTHER_USER_ID).build();
      given(commentRepository.findById(COMMENT_ID)).willReturn(Optional.of(comment));
      given(likeRepository.findByBattleItemCommentIdAndCreatorId(COMMENT_ID, OTHER_USER_ID))
          .willReturn(Optional.of(existing));

      // when
      BattleItemCommentResponse.LikeResult result = service.toggleLike(COMMENT_ID, OTHER_USER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getIsLiked()).as("공감 취소됨").isFalse();
        softly.assertThat(result.getLikeCount()).isEqualTo(previousLike - 1);
        softly.assertThat(comment.getLikeCount()).isEqualTo(previousLike - 1);
      });
      then(likeRepository).should().delete(existing);
      then(likeRepository).should(never()).save(any());
    }
  }

  // ==================== helpers ====================

  private BattleItemComment commentBy(Long writerId, String content) {
    Battle battle = BattleFixture.active();
    BattleItem item = BattleItemFixture.custom(battle, "아이템");
    return BattleItemComment.builder()
        .id(COMMENT_ID).battleItem(item).creatorId(writerId)
        .creatorNickname(NICKNAME).content(content).isDeleted(false)
        .build();
  }

  private ExternalUserInfo externalUser() {
    return ExternalUserInfo.builder()
        .userId(WRITER_ID)
        .nickname(NICKNAME)
        .build();
  }

  private BattleItemCommentRequest.Create createRequest(String content) {
    return new BattleItemCommentRequest.Create(content);
  }

  private BattleItemCommentRequest.Update updateRequest(String content) {
    return new BattleItemCommentRequest.Update(content);
  }
}
