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
public enum BattleItemType {
  PRODUCT("PRODUCT", "제품"),
  CUSTOM("CUSTOM", "사용자 직접입력"),
  YOUTUBE("YOUTUBE", "유튜브"),
  ;

  private String title;
  private String description;

  public static BattleItemType getBattleItemType(String title) {
    return Arrays.stream(values())
        .filter(type -> type.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid BattleItemType: " + title));
  }
}