package com.example.toycontent.app.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest.CommentCreate;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest.CommentUpdate;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedComment;
import com.example.toycontent.app.feed.repository.FeedCommentRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.FeedFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedCommentService")
class FeedCommentServiceTest {

  private static final long FEED_ID = 1L;
  private static final long COMMENT_ID = 10L;
  private static final long FEED_OWNER_ID = 100L;
  private static final long COMMENTER_ID = 200L;
  private static final String COMMENTER_NICKNAME = "댓글유저";

  @Mock private FeedRepository feedRepository;
  @Mock private FeedCommentRepository feedCommentRepository;
  @Mock private ExternalUserInfoService externalUserInfoService;
  @Mock private NotificationService notificationService;
  @Mock private ExpGrantService expGrantService;

  @InjectMocks private FeedCommentService feedCommentService;

  @Nested
  @DisplayName("createComment - 댓글 생성")
  class CreateComment {

    @Test
    @DisplayName("댓글 생성 시 피드의 댓글 수가 1 증가한다")
    void 댓글수_증가() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      int previousCount = feed.getCommentCount();
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(externalUserInfoService.getUserInfo(COMMENTER_ID))
          .willReturn(externalUser(COMMENTER_NICKNAME));

      // when
      feedCommentService.createComment(FEED_ID, new CommentCreate("좋은 글!", null), COMMENTER_ID);

      // then
      assertThat(feed.getCommentCount())
          .as("댓글 생성 후 피드의 댓글 수")
          .isEqualTo(previousCount + 1);
    }

    @Test
    @DisplayName("댓글 생성 시 피드 작성자에게 알림을 발송한다")
    void 알림_발송() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(externalUserInfoService.getUserInfo(COMMENTER_ID))
          .willReturn(externalUser(COMMENTER_NICKNAME));

      // when
      feedCommentService.createComment(FEED_ID, new CommentCreate("좋은 글!", null), COMMENTER_ID);

      // then
      then(notificationService).should().notifyFeedComment(
          any(), any(), any(), any(), any(), any()
      );
    }

    @Test
    @DisplayName("댓글 내용, 작성자 정보가 엔티티에 정확히 저장된다")
    void 엔티티_필드_검증() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(externalUserInfoService.getUserInfo(COMMENTER_ID))
          .willReturn(externalUser(COMMENTER_NICKNAME));

      // when
      feedCommentService.createComment(FEED_ID, new CommentCreate("테스트 댓글", null), COMMENTER_ID);

      // then
      ArgumentCaptor<FeedComment> captor = ArgumentCaptor.forClass(FeedComment.class);
      then(feedCommentRepository).should().save(captor.capture());
      FeedComment saved = captor.getValue();
      assertSoftly(softly -> {
        softly.assertThat(saved.getContent()).isEqualTo("테스트 댓글");
        softly.assertThat(saved.getCreatorId()).isEqualTo(COMMENTER_ID);
        softly.assertThat(saved.getCreatorNickname()).isEqualTo(COMMENTER_NICKNAME);
        softly.assertThat(saved.getDeleted()).isFalse();
      });
    }

    @Test
    @DisplayName("존재하지 않는 피드에 댓글 작성 시 RestApiException을 던진다")
    void 피드_없음_예외() {
      // given
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          feedCommentService.createComment(FEED_ID, new CommentCreate("...", null), COMMENTER_ID))
          .isInstanceOf(RestApiException.class);

      then(feedCommentRepository).shouldHaveNoInteractions();
      then(notificationService).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("updateComment - 댓글 수정")
  class UpdateComment {

    @Test
    @DisplayName("댓글 내용이 수정된다")
    void 내용_변경() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      FeedComment comment = FeedComment.builder()
          .id(COMMENT_ID).feed(feed).creatorId(COMMENTER_ID).content("원본 댓글")
          .build();
      given(feedCommentRepository.findByIdAndFeedId(COMMENT_ID, FEED_ID))
          .willReturn(Optional.of(comment));

      // when
      feedCommentService.updateComment(FEED_ID, COMMENT_ID, new CommentUpdate("수정된 댓글"));

      // then
      assertThat(comment.getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 RestApiException을 던진다")
    void 댓글_없음_예외() {
      // given
      given(feedCommentRepository.findByIdAndFeedId(COMMENT_ID, FEED_ID))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          feedCommentService.updateComment(FEED_ID, COMMENT_ID, new CommentUpdate("...")))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("deleteComment - 댓글 삭제")
  class DeleteComment {

    @Test
    @DisplayName("댓글 삭제 시 논리삭제 되고 피드의 댓글 수가 감소한다")
    void 논리삭제_및_카운트_감소() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      feed.incrementCommentCount();
      feed.incrementCommentCount();
      int previousCount = feed.getCommentCount();
      FeedComment comment = FeedComment.builder()
          .id(COMMENT_ID).feed(feed).creatorId(COMMENTER_ID).content("댓글")
          .deleted(false)
          .build();
      given(feedCommentRepository.findByIdAndFeedId(COMMENT_ID, FEED_ID))
          .willReturn(Optional.of(comment));

      // when
      feedCommentService.deleteComment(FEED_ID, COMMENT_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(comment.getDeleted()).as("논리삭제 플래그").isTrue();
        softly.assertThat(comment.getDeletedAt()).as("삭제 시각").isNotNull();
        softly.assertThat(feed.getCommentCount())
            .as("피드의 댓글 수는 1 감소").isEqualTo(previousCount - 1);
      });
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 RestApiException을 던진다")
    void 댓글_없음_예외() {
      // given
      given(feedCommentRepository.findByIdAndFeedId(COMMENT_ID, FEED_ID))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedCommentService.deleteComment(FEED_ID, COMMENT_ID))
          .isInstanceOf(RestApiException.class);
    }
  }

  private ExternalUserInfo externalUser(String nickname) {
    return ExternalUserInfo.builder()
        .userId(COMMENTER_ID)
        .nickname(nickname)
        .build();
  }
}
