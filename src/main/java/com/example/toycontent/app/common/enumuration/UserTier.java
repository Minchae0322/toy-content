package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum UserTier {

  PLAIN("PLAIN", "플레인", 1, 5),
  FRUITY("FRUITY", "프루티", 6, 15),
  GRANOLA("GRANOLA", "그래놀라", 16, 25),
  PARFAIT("PARFAIT", "파르페", 26, 35),
  SIGNATURE("SIGNATURE", "시그니처", 36, 40);

  private final String code;
  private final String description;
  private final int minLevel;
  private final int maxLevel;

  public static UserTier fromLevel(int level) {
    for (UserTier tier : values()) {
      if (level >= tier.minLevel && level <= tier.maxLevel) {
        return tier;
      }
    }
    return SIGNATURE;
  }
}
