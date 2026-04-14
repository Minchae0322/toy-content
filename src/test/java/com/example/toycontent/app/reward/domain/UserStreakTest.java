package com.example.toycontent.app.reward.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.example.toycontent.support.fixture.UserStreakFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserStreak 도메인")
class UserStreakTest {

  @Nested
  @DisplayName("recordPosting - 인증 작성 기록")
  class RecordPosting {

    @Test
    @DisplayName("첫 작성이면 currentStreak이 1이 된다")
    void 첫_작성() {
      // given
      UserStreak streak = UserStreakFixture.fresh();

      // when
      boolean recorded = streak.recordPosting(LocalDate.now());

      // then
      assertSoftly(softly -> {
        softly.assertThat(recorded).as("기록 성공").isTrue();
        softly.assertThat(streak.getCurrentStreak()).as("현재 스트릭").isEqualTo(1);
        softly.assertThat(streak.getMaxStreak()).as("최대 스트릭").isEqualTo(1);
      });
    }

    @Test
    @DisplayName("연속일이면 currentStreak이 증가한다")
    void 연속_작성() {
      // given
      UserStreak streak = UserStreakFixture.withStreak(5);

      // when
      boolean recorded = streak.recordPosting(LocalDate.now());

      // then
      assertSoftly(softly -> {
        softly.assertThat(recorded).as("기록 성공").isTrue();
        softly.assertThat(streak.getCurrentStreak()).as("현재 스트릭").isEqualTo(6);
        softly.assertThat(streak.getMaxStreak()).as("최대 스트릭").isEqualTo(6);
      });
    }

    @Test
    @DisplayName("같은 날 재작성이면 false를 반환하고 변경 없음")
    void 같은날_중복() {
      // given
      UserStreak streak = UserStreakFixture.withStreak(3);
      streak.recordPosting(LocalDate.now());

      // when
      boolean recorded = streak.recordPosting(LocalDate.now());

      // then
      assertThat(recorded).as("중복 기록").isFalse();
    }

    @Test
    @DisplayName("하루 이상 건너뛰면 currentStreak이 1로 리셋된다")
    void 갭_발생_리셋() {
      // given
      UserStreak streak = UserStreak.builder()
          .id(1L).userId(100L)
          .currentStreak(10).maxStreak(10)
          .lastPostedDate(LocalDate.now().minusDays(3))
          .recoveryTickets(0)
          .build();

      // when
      streak.recordPosting(LocalDate.now());

      // then
      assertSoftly(softly -> {
        softly.assertThat(streak.getCurrentStreak()).as("리셋 후 스트릭").isEqualTo(1);
        softly.assertThat(streak.getMaxStreak()).as("최대 스트릭 유지").isEqualTo(10);
      });
    }
  }

  @Nested
  @DisplayName("useRecoveryTicket - 복구 티켓 사용")
  class UseRecoveryTicket {

    @Test
    @DisplayName("복구 티켓을 사용하면 스트릭이 1 증가하고 티켓이 1 감소한다")
    void 복구_티켓_사용() {
      // given
      UserStreak streak = UserStreakFixture.withRecoveryTickets(2);
      int previousStreak = streak.getCurrentStreak();

      // when
      streak.useRecoveryTicket();

      // then
      assertSoftly(softly -> {
        softly.assertThat(streak.getCurrentStreak()).as("스트릭 증가").isEqualTo(previousStreak + 1);
        softly.assertThat(streak.getRecoveryTickets()).as("티켓 감소").isEqualTo(1);
      });
    }
  }
}
