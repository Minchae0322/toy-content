package com.example.toycontent.app.feed.controller.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 피드 스크롤(/feeds/scroll)의 키셋 커서 — (created_at, id) 두 값을 문자열 하나로 묶는다.
 *
 * <p>정렬이 {@code created_at DESC, id DESC}라 커서도 그 두 값이어야 한다. id만 쓰면
 * 적재 데이터처럼 id 순서와 작성일 순서가 어긋날 때 최신순이 깨진다. 같은 시각이 여러 건이면
 * id가 동점을 가르고, 커서 조건은 {@code created_at < c OR (created_at = c AND id < cid)}다.
 *
 * <p>형식: {@code <ISO LocalDateTime>_<id>} (예 {@code 2026-09-05T18:00:00.123456_12345}).
 * 컬럼이 datetime(6)이라 마이크로초까지 그대로 실어야 경계 행이 빠지지 않는다.
 * 클라이언트는 응답의 nextCursor를 해석하지 않고 그대로 돌려보낸다.
 */
public record FeedCursor(LocalDateTime createdAt, Long id) {

  private static final char SEP = '_';

  public static String encode(LocalDateTime createdAt, Long id) {
    return createdAt.toString() + SEP + id;
  }

  /** 비어 있으면 null(첫 페이지). 형식이 틀리면 IllegalArgumentException → 400. */
  public static FeedCursor parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    int sep = raw.lastIndexOf(SEP);
    if (sep <= 0 || sep == raw.length() - 1) {
      throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다: " + raw);
    }
    try {
      return new FeedCursor(
          LocalDateTime.parse(raw.substring(0, sep)),
          Long.parseLong(raw.substring(sep + 1)));
    } catch (DateTimeParseException | NumberFormatException e) {
      throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다: " + raw, e);
    }
  }
}
