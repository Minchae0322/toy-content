package com.example.toycontent.app.battle.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.support.fixture.BattleFixture;
import com.example.toycontent.support.fixture.BattleItemFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BattleItem 도메인")
class BattleItemTest {

  @Nested
  @DisplayName("신고 누적에 따른 상태 전환")
  class ReportAndStatus {

    @Test
    @DisplayName("신고 2회까지는 ACTIVE 상태를 유지한다")
    void report_2회_상태_유지() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // when
      item.incrementReport();
      item.incrementReport();

      // then
      assertSoftly(softly -> {
        softly.assertThat(item.getReportCount()).isEqualTo(2);
        softly.assertThat(item.getStatus()).isEqualTo(BattleItemStatus.ACTIVE);
      });
    }

    @Test
    @DisplayName("신고가 3회 누적되면 UNDER_REVIEW 상태로 자동 전환된다")
    void report_3회_자동_검토중_전환() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // when
      item.incrementReport();
      item.incrementReport();
      item.incrementReport();

      // then
      assertSoftly(softly -> {
        softly.assertThat(item.getReportCount()).isEqualTo(3);
        softly.assertThat(item.getStatus()).isEqualTo(BattleItemStatus.UNDER_REVIEW);
      });
    }

    @Test
    @DisplayName("exclude() 호출 시 상태가 EXCLUDED로 전환된다")
    void exclude_상태_전환() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // when
      item.exclude();

      // then
      assertThat(item.getStatus()).isEqualTo(BattleItemStatus.EXCLUDED);
    }

    @Test
    @DisplayName("approve() 호출 시 상태가 ACTIVE로 복귀한다")
    void approve_상태_복귀() {
      // given
      BattleItem item = BattleItemFixture.withStatus(
          BattleFixture.active(), BattleItemStatus.UNDER_REVIEW);

      // when
      item.approve();

      // then
      assertThat(item.getStatus()).isEqualTo(BattleItemStatus.ACTIVE);
    }
  }

  @Nested
  @DisplayName("투표 가능 여부 판정")
  class CanVote {

    @Test
    @DisplayName("ACTIVE 상태이고 삭제되지 않았으면 투표 가능하다")
    void active_상태_투표_가능() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // expect
      assertThat(item.canVote()).isTrue();
    }

    @Test
    @DisplayName("UNDER_REVIEW 상태면 투표 불가능하다")
    void under_review_상태_투표_불가() {
      // given
      BattleItem item = BattleItemFixture.withStatus(
          BattleFixture.active(), BattleItemStatus.UNDER_REVIEW);

      // expect
      assertThat(item.canVote()).isFalse();
    }

    @Test
    @DisplayName("EXCLUDED 상태면 투표 불가능하다")
    void excluded_상태_투표_불가() {
      // given
      BattleItem item = BattleItemFixture.withStatus(
          BattleFixture.active(), BattleItemStatus.EXCLUDED);

      // expect
      assertThat(item.canVote()).isFalse();
    }

    @Test
    @DisplayName("삭제된 아이템은 ACTIVE여도 투표 불가능하다")
    void 삭제된_아이템_투표_불가() {
      // given
      BattleItem item = BattleItemFixture.deleted(BattleFixture.active());

      // expect
      assertThat(item.canVote()).isFalse();
    }
  }

  @Nested
  @DisplayName("점수 가산/차감")
  class Score {

    @Test
    @DisplayName("addScore() 후 subtractScore()를 같은 값으로 호출하면 원상복구된다")
    void 점수_증감_쌍() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");
      item.addScore(3);

      // when
      item.subtractScore(3);

      // then
      assertThat(item.getTotalScore()).isZero();
    }

    @Test
    @DisplayName("subtractScore()는 음수로 떨어지지 않는다")
    void 점수_하한_보호() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");
      item.addScore(2);

      // when
      item.subtractScore(10);

      // then
      assertThat(item.getTotalScore()).isZero();
    }

    @Test
    @DisplayName("decrementVoteCount()는 음수로 떨어지지 않는다")
    void 투표수_하한_보호() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // when
      item.decrementVoteCount();

      // then
      assertThat(item.getVoteCount()).isZero();
    }
  }

  @Nested
  @DisplayName("타입별 표시 이름 / 이미지 URL")
  class DisplayFields {

    @Test
    @DisplayName("CUSTOM 아이템은 customName을 표시 이름으로 사용한다")
    void custom_이름() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "나만의 브랜드");

      // expect
      assertThat(item.getDisplayName()).isEqualTo("나만의 브랜드");
    }

    @Test
    @DisplayName("CUSTOM 아이템은 customImageUrl을 표시 이미지로 사용한다")
    void custom_이미지() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // expect
      assertThat(item.getDisplayImageUrl()).isEqualTo("https://example.com/custom.png");
    }

    @Test
    @DisplayName("YOUTUBE 아이템은 contentId 기반 썸네일 URL을 반환한다")
    void youtube_썸네일_생성() {
      // given
      BattleItem item = BattleItemFixture.youtube(BattleFixture.active(), "abc123");

      // expect
      assertThat(item.getDisplayImageUrl())
          .isEqualTo("https://img.youtube.com/vi/abc123/hqdefault.jpg");
    }

    @Test
    @DisplayName("YOUTUBE 아이템은 contentId 기반 embed URL을 반환한다")
    void youtube_embed_URL() {
      // given
      BattleItem item = BattleItemFixture.youtube(BattleFixture.active(), "abc123");

      // expect
      assertThat(item.getEmbedUrl())
          .isEqualTo("https://www.youtube.com/embed/abc123");
    }

    @Test
    @DisplayName("CUSTOM 아이템의 embed URL은 null이다")
    void custom_embed_URL_없음() {
      // given
      BattleItem item = BattleItemFixture.custom(BattleFixture.active(), "아이템");

      // expect
      assertThat(item.getEmbedUrl()).isNull();
    }
  }
}
