package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.hotscore.HotScoreFormula;
import com.example.toycontent.app.common.hotscore.HotScoreSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.support.fixture.BattleFixture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Battle 도메인")
class BattleTest {

  @Nested
  @DisplayName("투표/참여자/점수 카운터 증감")
  class CounterMutations {

    @Test
    @DisplayName("addTotalVotes()는 인자 값만큼 totalVotes를 증가시킨다")
    void addTotalVotes_증가() {
      // given
      Battle battle = BattleFixture.active();

      // when
      battle.addTotalVotes(3);

      // then
      assertThat(battle.getTotalVotes()).isEqualTo(3);
    }

    @Test
    @DisplayName("subtractTotalVotes()는 음수로 떨어지지 않는다")
    void subtractTotalVotes_하한_보호() {
      // given
      Battle battle = BattleFixture.active();
      battle.addTotalVotes(2);

      // when
      battle.subtractTotalVotes(5);

      // then
      assertThat(battle.getTotalVotes())
          .as("투표 수는 0 미만으로 내려가지 않아야 한다")
          .isZero();
    }

    @Test
    @DisplayName("incrementTotalParticipants()는 참여자 수를 1 증가시킨다")
    void incrementTotalParticipants() {
      // given
      Battle battle = BattleFixture.active();

      // when
      battle.incrementTotalParticipants();
      battle.incrementTotalParticipants();

      // then
      assertThat(battle.getTotalParticipants()).isEqualTo(2);
    }

    @Test
    @DisplayName("decrementTotalParticipants()는 음수로 떨어지지 않는다")
    void decrementTotalParticipants_하한_보호() {
      // given
      Battle battle = BattleFixture.active();

      // when
      battle.decrementTotalParticipants();

      // then
      assertThat(battle.getTotalParticipants()).isZero();
    }

    @Test
    @DisplayName("addTotalScore()와 subtractTotalScore()가 쌍으로 적용되면 원상복구된다")
    void 점수_증감_쌍() {
      // given
      Battle battle = BattleFixture.active();

      // when
      battle.addTotalScore(10);
      battle.addTotalScore(5);
      battle.subtractTotalScore(10);

      // then
      assertThat(battle.getTotalScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("subtractTotalScore()는 음수로 떨어지지 않는다")
    void subtractTotalScore_하한_보호() {
      // given
      Battle battle = BattleFixture.active();
      battle.addTotalScore(3);

      // when
      battle.subtractTotalScore(10);

      // then
      assertThat(battle.getTotalScore()).isZero();
    }

    @Test
    @DisplayName("decrementTotalCommentCount()는 0 이하로 떨어지지 않는다")
    void 댓글수_하한_보호() {
      // given
      Battle battle = BattleFixture.active();
      battle.incrementTotalCommentCount();

      // when
      battle.decrementTotalCommentCount();
      battle.decrementTotalCommentCount();

      // then
      assertThat(battle.getTotalCommentCount()).isZero();
    }
  }

  @Nested
  @DisplayName("투표 타입 판별")
  class VoteTypeCheck {

    @Test
    @DisplayName("SINGLE 타입이면 isSingleVote()가 true를 반환한다")
    void isSingleVote_true() {
      // given
      Battle battle = Battle.builder()
          .id(1L)
          .title("단일 투표 배틀")
          .creatorId(100L)
          .startDate(LocalDateTime.now().minusDays(1))
          .participationStartDate(LocalDateTime.now().minusDays(1))
          .endDate(LocalDateTime.now().plusDays(1))
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.SINGLE)
          .status(BattleStatus.NORMAL)
          .build();

      // when
      boolean result = battle.isSingleVote();

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("MULTIPLE 타입이면 isSingleVote()가 false를 반환한다")
    void isSingleVote_false() {
      // given
      Battle battle = BattleFixture.active(); // MULTIPLE

      // when
      boolean result = battle.isSingleVote();

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("핫 스코어 계산")
  class HotScoreCalculation {

    @Test
    @DisplayName("활동이 없으면 핫 스코어는 시간 항(시작시각/상수)만 남는다 — 새 배틀도 0이 아니라 위에서 시작")
    void hotScore_활동없음() {
      // given
      Battle battle = BattleFixture.active();

      // when
      double score = battle.calculateHotScore();

      // then
      double timeTermOnly = HotScoreFormula.score(0, battle.getStartDate(), HotScoreSettings.battleDivisor());
      assertThat(score).isEqualTo(timeTermOnly).isPositive();
    }

    @Test
    @DisplayName("참여도 10배는 시간 상수(30일)만큼의 시각 차이와 같다")
    void hotScore_참여도_10배는_30일() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Battle old = newBattle(now.minusSeconds(HotScoreSettings.battleDivisor()));
      Battle recent = newBattle(now);
      old.addTotalVotes(50);      // 참여도 100
      recent.addTotalVotes(5);    // 참여도 10

      // then
      assertThat(old.calculateHotScore()).isCloseTo(recent.calculateHotScore(), within(1e-6));
    }

    @Test
    @DisplayName("활동량이 동일할 때 최근 배틀이 오래된 배틀보다 높은 핫 스코어를 갖는다")
    void hotScore_최신_배틀_가중치_높음() {
      // given
      LocalDateTime now = LocalDateTime.now();
      Battle recent = newBattle(now.minusHours(1));
      Battle old = newBattle(now.minusDays(30));
      recent.addTotalVotes(50);
      recent.incrementTotalParticipants();
      old.addTotalVotes(50);
      old.incrementTotalParticipants();

      // when
      double recentScore = recent.calculateHotScore();
      double oldScore = old.calculateHotScore();

      // then
      assertThat(recentScore)
          .as("동일 활동량 기준, 최신 배틀이 더 높은 핫 스코어를 가져야 한다")
          .isGreaterThan(oldScore);
    }

    @Test
    @DisplayName("updateHotScore() 호출 시 hotScore와 hotScoreUpdatedAt이 설정된다")
    void updateHotScore_필드_반영() {
      // given
      Battle battle = BattleFixture.active();
      battle.addTotalVotes(10);

      // when
      battle.updateHotScore();

      // then
      assertSoftly(softly -> {
        softly.assertThat(battle.getHotScore()).isPositive();
        softly.assertThat(battle.getHotScoreUpdatedAt()).isNotNull();
      });
    }

    private Battle newBattle(LocalDateTime startDate) {
      return Battle.builder()
          .title("배틀")
          .creatorId(100L)
          .startDate(startDate)
          .participationStartDate(startDate)
          .endDate(startDate.plusDays(7))
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.MULTIPLE)
          .status(BattleStatus.NORMAL)
          .build();
    }
  }
}
