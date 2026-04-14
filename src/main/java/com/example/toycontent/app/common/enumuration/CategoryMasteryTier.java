package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CategoryMasteryTier {

  NONE("NONE", "미달성", 0),
  INTERESTED("INTERESTED", "관심자", 1),
  ENTHUSIAST("ENTHUSIAST", "애호가", 2),
  CURATOR("CURATOR", "큐레이터", 3),
  EXPERT("EXPERT", "전문가", 4);

  private final String code;
  private final String description;
  private final int order;

  public static CategoryMasteryTier ofCode(String inputCode) {
    return Arrays.stream(values())
        .filter(v -> v.code.equalsIgnoreCase(inputCode))
        .findAny()
        .orElseThrow(
            () -> new IllegalArgumentException(
                String.format("올바르지 않은 CategoryMasteryTier 코드입니다. 입력값: %s", inputCode)));
  }
}
