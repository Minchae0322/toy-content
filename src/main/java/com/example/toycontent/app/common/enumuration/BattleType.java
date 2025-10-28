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
public enum BattleType {
  OPEN("OPEN", "오픈 배틀"),
  CURATION("CURATION", "큐레이션 배틀");

  private String title;
  private String description;

  public static BattleType getBattleType(String title) {
    return Arrays.stream(values())
        .filter(type -> type.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid BattleType: " + title));
  }
}