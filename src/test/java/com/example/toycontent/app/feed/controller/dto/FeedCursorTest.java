package com.example.toycontent.app.feed.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FeedCursorTest {

  @Test
  void 마이크로초까지_왕복된다() {
    LocalDateTime at = LocalDateTime.of(2026, 9, 5, 18, 0, 0, 123_456_000);
    String encoded = FeedCursor.encode(at, 12345L);

    assertThat(encoded).isEqualTo("2026-09-05T18:00:00.123456_12345");
    assertThat(FeedCursor.parse(encoded)).isEqualTo(new FeedCursor(at, 12345L));
  }

  @Test
  void 초_단위_시각도_왕복된다() {
    LocalDateTime at = LocalDateTime.of(2026, 9, 5, 18, 0);
    assertThat(FeedCursor.parse(FeedCursor.encode(at, 1L))).isEqualTo(new FeedCursor(at, 1L));
  }

  @Test
  void 비어있으면_첫_페이지() {
    assertThat(FeedCursor.parse(null)).isNull();
    assertThat(FeedCursor.parse("  ")).isNull();
  }

  @Test
  void 형식이_틀리면_IllegalArgumentException() {
    assertThatThrownBy(() -> FeedCursor.parse("95")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FeedCursor.parse("abc_12")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FeedCursor.parse("2026-09-05T18:00:00_x")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FeedCursor.parse("2026-09-05T18:00:00_")).isInstanceOf(IllegalArgumentException.class);
  }
}
