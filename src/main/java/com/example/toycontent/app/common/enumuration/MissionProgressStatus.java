package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MissionProgressStatus {

  IN_PROGRESS("IN_PROGRESS", "진행 중"),
  COMPLETED("COMPLETED", "완료"),
  CLAIMED("CLAIMED", "보상 수령 완료"),
  EXPIRED("EXPIRED", "만료");

  private final String code;
  private final String description;

  public static MissionProgressStatus ofCode(String inputCode) {
    return Arrays.stream(values())
        .filter(v -> v.code.equalsIgnoreCase(inputCode))
        .findAny()
        .orElseThrow(
            () -> new IllegalArgumentException(
                String.format("올바르지 않은 MissionProgressStatus 코드입니다. 입력값: %s", inputCode)));
  }
}
