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
public enum BattleItemStatus {
  ACTIVE("ACTIVE", "활성"),
  UNDER_REVIEW("UNDER_REVIEW", "검토중"),
  EXCLUDED("EXCLUDED", "제외됨");

  private String title;
  private String description;

  public static BattleItemStatus getBattleItemStatus(String title) {
    return Arrays.stream(values())
        .filter(status -> status.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid BattleItemStatus: " + title));
  }
}
