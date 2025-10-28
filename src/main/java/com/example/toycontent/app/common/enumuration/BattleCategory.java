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
public enum BattleCategory {
  SNEAKERS("SNEAKERS", "스니커즈"),
  TECH("TECH", "테크"),
  BEAUTY("BEAUTY", "뷰티"),
  FNB("FNB", "식음료"),
  FASHION("FASHION", "패션"),
  LIVING("LIVING", "리빙"),
  STATIONERY("STATIONERY", "문구"),
  ETC("ETC", "기타");

  private String title;
  private String description;

  public static BattleCategory getBattleCategory(String title) {
    return Arrays.stream(values())
        .filter(category -> category.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid BattleCategory: " + title));
  }
}
