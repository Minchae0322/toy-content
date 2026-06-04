package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스와이프 배틀의 1회 평가 결과. 단순 합산 랭킹: {@code STRONG_PICK*3 + PICK*1}.
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SwipeVerdict {
  STRONG_PICK("강추 PICK", 3),
  PICK("PICK", 1),
  PASS("PASS", 0);

  private final String label;
  private final int score;
}
