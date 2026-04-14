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
  private static final int TRENDING_THRESHOLD = 100;

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

  @Nested
  @DisplayName("트렌딩 판정")
  class TrendingCheck {

    @Test
    @DisplayName("24시간 전 조회수 스냅샷이 없으면 트렌딩이 아니다")
    void checkTrending_스냅샷_부재() {
      // given
      Feed feed = FeedFixture.basic();

      // when
      boolean trending = feed.checkTrending(TRENDING_THRESHOLD);

      // then
      assertThat(trending)
          .as("스냅샷이 없는 피드는 트렌딩으로 분류되지 않는다")
          .isFalse();
    }

    @Test
    @DisplayName("24시간 내 조회수 증가분이 임계값 이상이면 트렌딩이다")
    void checkTrending_증가분_임계값_이상() {
      // given — 1000 -> 1150 (증가분 150, 임계 100)
      Feed feed = FeedFixture.withViewCounts(1150, 1000);

      // when
      boolean trending = feed.checkTrending(TRENDING_THRESHOLD);

      // then
      assertThat(trending)
          .as("증가분이 임계값 이상일 때 트렌딩")
          .isTrue();
    }

    @Test
    @DisplayName("24시간 내 조회수 증가분이 임계값 미만이면 트렌딩이 아니다")
    void checkTrending_증가분_임계값_미만() {
      // given — 1000 -> 1050 (증가분 50, 임계 100)
      Feed feed = FeedFixture.withViewCounts(1050, 1000);

      // when
      boolean trending = feed.checkTrending(TRENDING_THRESHOLD);

      // then
      assertThat(trending)
          .as("증가분이 임계값 미만일 때 비(非)트렌딩")
          .isFalse();
    }

    @Test
    @DisplayName("updateTrendingStatus()는 판정 결과를 isTrending 필드에 반영한다")
    void updateTrendingStatus_필드_반영() {
      // given
      Feed feed = FeedFixture.withViewCounts(1200, 1000);

      // when
      feed.updateTrendingStatus(TRENDING_THRESHOLD);

      // then
      assertThat(feed.getIsTrending())
          .as("트렌딩 상태가 필드에 반영된다")
          .isTrue();
    }
  }
}
