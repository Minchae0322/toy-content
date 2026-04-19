package com.example.toycontent.app.reward.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DailyExpCap")
class DailyExpCapTest {

  private static final long DAILY_LIMIT = 200L;

  private DailyExpCap createCap() {
    return DailyExpCap.builder()
        .userId(1L)
        .capDate(LocalDate.now())
        .build();
  }

  @Nested
  @DisplayName("consume - EXP 캡 소비")
  class Consume {

    @Test
    @DisplayName("한도 내 요청이면 전액 소비된다")
    void 한도_내_전액_소비() {
      DailyExpCap cap = createCap();

      long actual = cap.consume(50, DAILY_LIMIT);

      assertSoftly(softly -> {
        softly.assertThat(actual).as("실제 소비량").isEqualTo(50);
        softly.assertThat(cap.getUsedAmount()).as("사용량").isEqualTo(50);
      });
    }

    @Test
    @DisplayName("여러 번 소비해도 누적된다")
    void 누적_소비() {
      DailyExpCap cap = createCap();

      cap.consume(100, DAILY_LIMIT);
      long actual = cap.consume(80, DAILY_LIMIT);

      assertSoftly(softly -> {
        softly.assertThat(actual).as("실제 소비량").isEqualTo(80);
        softly.assertThat(cap.getUsedAmount()).as("누적 사용량").isEqualTo(180);
      });
    }

    @Test
    @DisplayName("한도를 초과하면 남은 양만큼만 소비된다")
    void 한도_초과_부분_소비() {
      DailyExpCap cap = createCap();

      cap.consume(180, DAILY_LIMIT);
      long actual = cap.consume(50, DAILY_LIMIT);

      assertSoftly(softly -> {
        softly.assertThat(actual).as("실제 소비량").isEqualTo(20);
        softly.assertThat(cap.getUsedAmount()).as("누적 사용량").isEqualTo(200);
      });
    }

    @Test
    @DisplayName("한도를 이미 다 쓰면 0을 반환한다")
    void 한도_소진() {
      DailyExpCap cap = createCap();

      cap.consume(200, DAILY_LIMIT);
      long actual = cap.consume(10, DAILY_LIMIT);

      assertThat(actual).as("실제 소비량").isEqualTo(0);
    }
  }
}
