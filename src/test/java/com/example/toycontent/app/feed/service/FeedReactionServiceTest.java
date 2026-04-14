package com.example.toycontent.app.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.feed.controller.dto.FeedReactionResponse.ReactionResult;
import com.example.toycontent.app.feed.controller.dto.FeedReactionResponse.UserReactions;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReaction;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.FeedFixture;
import com.example.toycontent.support.fixture.FeedReactionFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedReactionService")
class FeedReactionServiceTest {

  private static final long FEED_ID = 1L;
  private static final long FEED_OWNER_ID = 100L;
  private static final long ACTOR_USER_ID = 200L;
  private static final String ACTOR_NICKNAME = "리액션유저";

  @Mock private FeedRepository feedRepository;
  @Mock private FeedReactionRepository feedReactionRepository;
  @Mock private NotificationService notificationService;
  @Mock private ExternalUserInfoService externalUserInfoService;

  @InjectMocks private FeedReactionService feedReactionService;

  @Nested
  @DisplayName("toggleReaction - 리액션 토글")
  class ToggleReaction {

    @Test
    @DisplayName("기존 리액션이 없으면 새로 추가하고 작성자에게 알림을 발송한다")
    void 신규_리액션_추가() {
      // given
      Feed feed = FeedFixture.withLikeCount(5);
      given(feedRepository.findByIdWithPessimisticLock(FEED_ID))
          .willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndReactionType(
          FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE))
          .willReturn(Optional.empty());
      given(externalUserInfoService.getUserInfo(ACTOR_USER_ID))
          .willReturn(externalUser(ACTOR_NICKNAME));

      // when
      ReactionResult result = feedReactionService
          .toggleReaction(FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getAction())
            .as("수행된 액션")
            .isEqualTo("added");
        softly.assertThat(result.getLikeCount())
            .as("증가한 좋아요 수")
            .isEqualTo(6);
        softly.assertThat(result.getReactionType())
            .as("리액션 타입")
            .isEqualTo(FeedReactionType.LIKE);
      });

      then(feedReactionRepository).should().save(any(FeedReaction.class));
      then(feedRepository).should().save(feed);
      then(notificationService).should()
          .notifyFeedLike(
              eq(feed.getUserId()),
              eq(ACTOR_USER_ID),
              eq(ACTOR_NICKNAME),
              any(),
              eq(FEED_ID),
              any());
    }

    @Test
    @DisplayName("기존 리액션이 있으면 제거하고 알림은 발송하지 않는다")
    void 기존_리액션_제거() {
      // given
      Feed feed = FeedFixture.withLikeCount(5);
      FeedReaction existing = FeedReactionFixture.like(feed);
      feed.addReaction(ACTOR_USER_ID, FeedReactionType.LIKE); // feed 내부 상태 정합성
      given(feedRepository.findByIdWithPessimisticLock(FEED_ID))
          .willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndReactionType(
          FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE))
          .willReturn(Optional.of(existing));

      // when
      ReactionResult result = feedReactionService
          .toggleReaction(FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getAction())
            .as("수행된 액션")
            .isEqualTo("removed");
        softly.assertThat(result.getReactionType())
            .as("리액션 타입")
            .isEqualTo(FeedReactionType.LIKE);
      });

      then(feedReactionRepository).should().delete(existing);
      then(feedRepository).should().save(feed);
      then(notificationService).should(never())
          .notifyFeedLike(anyLong(), anyLong(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("존재하지 않는 피드에 리액션을 시도하면 RestApiException을 던진다")
    void 피드_없음_예외() {
      // given
      given(feedRepository.findByIdWithPessimisticLock(FEED_ID))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          feedReactionService.toggleReaction(FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE))
          .isInstanceOf(RestApiException.class);

      then(feedReactionRepository).shouldHaveNoInteractions();
      then(notificationService).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("removeReaction - 특정 타입 제거")
  class RemoveReaction {

    @Test
    @DisplayName("대상 리액션이 존재하면 제거된다")
    void 정상_제거() {
      // given
      Feed feed = FeedFixture.withLikeCount(3);
      FeedReaction existing = FeedReactionFixture.like(feed);
      feed.addReaction(ACTOR_USER_ID, FeedReactionType.LIKE);
      given(feedRepository.findByIdWithPessimisticLock(FEED_ID))
          .willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndReactionType(
          FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE))
          .willReturn(Optional.of(existing));

      // when
      feedReactionService.removeReaction(FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE);

      // then
      then(feedReactionRepository).should().delete(existing);
      then(feedRepository).should().save(feed);
    }

    @Test
    @DisplayName("대상 리액션이 없으면 RestApiException을 던진다")
    void 리액션_없음_예외() {
      // given
      Feed feed = FeedFixture.withLikeCount(3);
      given(feedRepository.findByIdWithPessimisticLock(FEED_ID))
          .willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndReactionType(
          FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() ->
          feedReactionService.removeReaction(FEED_ID, ACTOR_USER_ID, FeedReactionType.LIKE))
          .isInstanceOf(RestApiException.class);

      then(feedReactionRepository).should(never()).delete(any());
    }
  }

  @Nested
  @DisplayName("getUserReactions - 유저 리액션 조회")
  class GetUserReactions {

    @Test
    @DisplayName("LIKE와 HOT 둘 다 있으면 hasLike=true, hasHot=true를 반환한다")
    void 둘_다_있음() {
      // given
      Feed feed = FeedFixture.basic();
      given(feedReactionRepository.findByFeedIdAndUserId(FEED_ID, ACTOR_USER_ID))
          .willReturn(List.of(
              FeedReactionFixture.like(feed),
              FeedReactionFixture.hot(feed)
          ));

      // when
      UserReactions reactions = feedReactionService.getUserReactions(FEED_ID, ACTOR_USER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(reactions.isHasLike()).isTrue();
        softly.assertThat(reactions.isHasHot()).isTrue();
      });
    }

    @Test
    @DisplayName("LIKE만 있으면 hasLike=true, hasHot=false를 반환한다")
    void LIKE만_있음() {
      // given
      Feed feed = FeedFixture.basic();
      given(feedReactionRepository.findByFeedIdAndUserId(FEED_ID, ACTOR_USER_ID))
          .willReturn(List.of(FeedReactionFixture.like(feed)));

      // when
      UserReactions reactions = feedReactionService.getUserReactions(FEED_ID, ACTOR_USER_ID);

      // then
      assertSoftly(softly -> {
        softly.assertThat(reactions.isHasLike()).isTrue();
        softly.assertThat(reactions.isHasHot()).isFalse();
      });
    }

    @Test
    @DisplayName("리액션이 하나도 없으면 둘 다 false를 반환한다")
    void 리액션_없음() {
      // given
      given(feedReactionRepository.findByFeedIdAndUserId(FEED_ID, ACTOR_USER_ID))
          .willReturn(List.of());

      // when
      UserReactions reactions = feedReactionService.getUserReactions(FEED_ID, ACTOR_USER_ID);

      // then
      assertThat(reactions.isHasLike()).isFalse();
      assertThat(reactions.isHasHot()).isFalse();
    }
  }

  private ExternalUserInfo externalUser(String nickname) {
    return ExternalUserInfo.builder()
        .userId(ACTOR_USER_ID)
        .nickname(nickname)
        .build();
  }
}
