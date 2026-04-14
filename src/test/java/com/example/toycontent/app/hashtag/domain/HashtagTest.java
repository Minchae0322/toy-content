package com.example.toycontent.app.hashtag.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Hashtag 도메인")
class HashtagTest {

  @Nested
  @DisplayName("사용 횟수 증감")
  class UsageCount {

    @Test
    @DisplayName("incrementUsageCount()는 usageCount를 1 증가시킨다")
    void increment() {
      // given
      Hashtag hashtag = Hashtag.builder().name("요거픽").usageCount(5L).build();

      // when
      hashtag.incrementUsageCount();

      // then
      assertThat(hashtag.getUsageCount()).isEqualTo(6L);
    }

    @Test
    @DisplayName("decrementUsageCount()는 usageCount를 1 감소시킨다")
    void decrement() {
      // given
      Hashtag hashtag = Hashtag.builder().name("요거픽").usageCount(5L).build();

      // when
      hashtag.decrementUsageCount();

      // then
      assertThat(hashtag.getUsageCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("decrementUsageCount()는 0 미만으로 떨어지지 않는다")
    void decrement_하한_보호() {
      // given
      Hashtag hashtag = Hashtag.builder().name("요거픽").usageCount(0L).build();

      // when
      hashtag.decrementUsageCount();

      // then
      assertThat(hashtag.getUsageCount())
          .as("사용 횟수는 음수가 되면 안 된다")
          .isZero();
    }
  }

  @Nested
  @DisplayName("이름 정규화")
  class NormalizeName {

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource({
        "'#요거픽',   '요거픽'",
        "' 요거픽 ', '요거픽'",
        "'#맛집#',    '맛집'",
        "'YOGURTTE',  'yogurtte'",
        "'#Best',     'best'"
    })
    @DisplayName("#과 공백이 제거되고 소문자로 변환된다")
    void 정규화_규칙(String input, String expected) {
      assertThat(Hashtag.normalizeName(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "null/empty/blank 입력 → null 반환")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("null / 빈 문자열 / 공백뿐이면 null을 반환한다")
    void 빈_입력_null_반환(String input) {
      assertThat(Hashtag.normalizeName(input)).isNull();
    }
  }
}
