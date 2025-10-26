package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FeedEvaluation {

  BEST("BEST", "강추"),
  GOOD("GOOD", "추천"),
  OKAY("OKAY", "괜찮음"),
  BAD("BAD", "비추천");

  private final String code;
  private final String description;

  public static FeedEvaluation ofCode(String inputCode) {
    return Arrays.stream(values())
        .filter(v -> v.code.equalsIgnoreCase(inputCode))
        .findAny()
        .orElseThrow(
            () -> new IllegalArgumentException(
                String.format("올바르지 않은 FeedEvaluation 코드입니다. 입력값: %s", inputCode)));
  }

  public static boolean isValid(String inputCode) {
    return Arrays.stream(values())
        .anyMatch(v -> v.code.equalsIgnoreCase(inputCode));
  }
}