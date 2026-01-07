package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum BattleStatus {
  NORMAL("NORMAL", "정상"),
  SUSPENDED("SUSPENDED", "정지됨"),
  EARLY_CLOSED("EARLY_CLOSED", "조기 종료"),
  ;

  private String title;
  private String description;

  public static BattleStatus getBattleStatus(String title) {
    return Arrays.stream(values())
        .filter(status -> status.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid BattleStatus: " + title));
  }
}
