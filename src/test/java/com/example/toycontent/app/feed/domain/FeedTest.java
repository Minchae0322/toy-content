package com.example.toycontent.app.feed.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.support.fixture.FeedFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Feed 도메인")
class FeedTest {

  private static final long ACTOR_USER_ID = 999L;
  @Nested
  @DisplayName("좋아요 수 증감")
  class LikeCountChange {

    @Test
    @DisplayName("incrementLikeCount()를 호출하면 좋아요 수가 1 증가한다")
    void incrementLikeCount_증가() {
      // given
      Feed feed = FeedFixture.withLikeCount(5);

      // when
      feed.incrementLikeCount();

      // then
      assertThat(feed.getLikeCount())
          .as("incrementLikeCount 호출 후 좋아요 수")
          .isEqualTo(6);
    }

    @Test
    @DisplayName("decrementLikeCount()를 호출하면 좋아요 수가 1 감소한다")
    void decrementLikeCount_감소() {
      // given
      Feed feed = FeedFixture.withLikeCount(5);

      // when
      feed.decrementLikeCount();

      // then
      assertThat(feed.getLikeCount())
          .as("decrementLikeCount 호출 후 좋아요 수")
          .isEqualTo(4);
    }

    @Test
    @DisplayName("좋아요 수가 0일 때 decrementLikeCount()를 호출해도 음수가 되지 않는다")
    void decrementLikeCount_하한_보호() {
      // given
      Feed feed = FeedFixture.withLikeCount(0);

      // when
      feed.decrementLikeCount();

      // then
      assertThat(feed.getLikeCount())
          .as("좋아요 수는 0 미만으로 내려가지 않아야 한다")
          .isZero();
    }
  }

  @Nested
  @DisplayName("리액션 추가 / 제거")
  class ReactionManagement {

    @Test
    @DisplayName("LIKE 리액션을 추가하면 좋아요 수가 증가하고, 리액션 목록에 포함된다")
    void addReaction_LIKE_좋아요_수_증가() {
      // given
      Feed feed = FeedFixture.withLikeCount(3);

      // when
      FeedReaction reaction = feed.addReaction(ACTOR_USER_ID, FeedReactionType.LIKE);

      // then
      assertSoftly(softly -> {
        softly.assertThat(feed.getLikeCount())
            .as("LIKE 추가 후 좋아요 수")
            .isEqualTo(4);
        softly.assertThat(feed.getReactions())
            .as("리액션 목록에 신규 리액션 포함")
            .contains(reaction);
        softly.assertThat(reaction.getUserId())
            .as("리액션을 누른 유저 ID")
            .isEqualTo(ACTOR_USER_ID);
        softly.assertThat(reaction.getReactionType())
            .as("리액션 타입")
            .isEqualTo(FeedReactionType.LIKE);
      });
    }

    @Test
    @DisplayName("HOT 리액션 추가는 likeCount에 영향을 주지 않는다")
    void addReaction_HOT_좋아요_수_변화없음() {
      // given
      Feed feed = FeedFixture.withLikeCount(3);

      // when
      feed.addReaction(ACTOR_USER_ID, FeedReactionType.HOT);

      // then
      assertThat(feed.getLikeCount())
          .as("HOT 리액션은 likeCount에 기여하지 않음")
          .isEqualTo(3);
    }

    @Test
    @DisplayName("LIKE 리액션을 제거하면 좋아요 수가 감소하고, 리액션 목록에서 빠진다")
    void removeReaction_LIKE_좋아요_수_감소() {
      // given
      Feed feed = FeedFixture.withLikeCount(5);
      FeedReaction reaction = feed.addReaction(ACTOR_USER_ID, FeedReactionType.LIKE);

      // when
      feed.removeReaction(reaction);

      // then
      assertSoftly(softly -> {
        softly.assertThat(feed.getLikeCount())
            .as("LIKE 제거 후 좋아요 수는 추가 전 값으로 복구")
            .isEqualTo(5);
        softly.assertThat(feed.getReactions())
            .as("리액션 목록에서 제거됨")
            .doesNotContain(reaction);
      });
    }

    @Test
    @DisplayName("HOT 리액션 제거는 likeCount에 영향을 주지 않는다")
    void removeReaction_HOT_좋아요_수_변화없음() {
      // given
      Feed feed = FeedFixture.withLikeCount(5);
      FeedReaction reaction = feed.addReaction(ACTOR_USER_ID, FeedReactionType.HOT);

      // when
      feed.removeReaction(reaction);

      // then
      assertThat(feed.getLikeCount())
          .as("HOT 제거 후 likeCount는 불변")
          .isEqualTo(5);
    }
  }

  @Nested
  @DisplayName("삭제 처리")
  class SoftDelete {

    @Test
    @DisplayName("delete() 호출 시 isDeleted가 true로, deletedAt이 현재 시각으로 설정된다")
    void delete_플래그_및_시각_세팅() {
      // given
      Feed feed = FeedFixture.basic();

      // when
      feed.delete();

      // then
      assertSoftly(softly -> {
        softly.assertThat(feed.getIsDeleted())
            .as("삭제 플래그")
            .isTrue();
        softly.assertThat(feed.getDeletedAt())
            .as("삭제 시각")
            .isNotNull();
      });
    }
  }

}
