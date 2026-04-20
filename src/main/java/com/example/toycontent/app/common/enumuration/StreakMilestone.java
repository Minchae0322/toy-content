package com.example.toycontent.app.common.enumuration;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StreakMilestone {

  DAY_3(3, 20L),
  DAY_7(7, 50L),
  DAY_14(14, 100L),
  DAY_30(30, 200L),
  DAY_100(100, 500L),
  ;

  private final int days;
  private final long bonusExp;

  public static Optional<StreakMilestone> from(int days) {
    return Arrays.stream(values())
        .filter(m -> m.days == days)
        .findFirst();
  }
}
