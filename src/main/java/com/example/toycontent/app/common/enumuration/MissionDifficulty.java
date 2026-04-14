package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MissionDifficulty {

  EASY("EASY", "쉬움", 20),
  NORMAL("NORMAL", "보통", 50),
  HARD("HARD", "어려움", 100);

  private final String code;
  private final String description;
  private final int baseExp;

  public static MissionDifficulty ofCode(String inputCode) {
    return Arrays.stream(values())
        .filter(v -> v.code.equalsIgnoreCase(inputCode))
        .findAny()
        .orElseThrow(
            () -> new IllegalArgumentException(
                String.format("올바르지 않은 MissionDifficulty 코드입니다. 입력값: %s", inputCode)));
  }
}
