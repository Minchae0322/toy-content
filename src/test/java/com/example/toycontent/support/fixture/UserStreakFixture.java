package com.example.toycontent.support.fixture;

import com.example.toycontent.app.reward.domain.UserStreak;
import java.time.LocalDate;

public class UserStreakFixture {

  public static final Long DEFAULT_USER_ID = 100L;

  private UserStreakFixture() {}

  public static UserStreak fresh() {
    return UserStreak.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .currentStreak(0)
        .maxStreak(0)
        .recoveryTickets(0)
        .build();
  }

  public static UserStreak withStreak(int streak) {
    return UserStreak.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .currentStreak(streak)
        .maxStreak(streak)
        .lastPostedDate(LocalDate.now().minusDays(1))
        .recoveryTickets(0)
        .build();
  }

  public static UserStreak withRecoveryTickets(int tickets) {
    return UserStreak.builder()
        .id(1L)
        .userId(DEFAULT_USER_ID)
        .currentStreak(3)
        .maxStreak(5)
        .lastPostedDate(LocalDate.now().minusDays(2))
        .recoveryTickets(tickets)
        .build();
  }
}
