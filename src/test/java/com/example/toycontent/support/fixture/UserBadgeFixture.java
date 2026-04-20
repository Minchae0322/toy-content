package com.example.toycontent.support.fixture;

import com.example.toycontent.app.reward.badge.domain.Badge;
import com.example.toycontent.app.reward.badge.domain.UserBadge;
import java.time.LocalDateTime;

public class UserBadgeFixture {

  public static final Long DEFAULT_USER_ID = 100L;
  public static final Long DEFAULT_USER_BADGE_ID = 1L;

  private UserBadgeFixture() {}

  public static UserBadge basic() {
    return UserBadge.builder()
        .id(DEFAULT_USER_BADGE_ID)
        .userId(DEFAULT_USER_ID)
        .badge(BadgeFixture.basic())
        .acquiredAt(LocalDateTime.now())
        .build();
  }

  public static UserBadge withBadge(Badge badge) {
    return UserBadge.builder()
        .id(DEFAULT_USER_BADGE_ID)
        .userId(DEFAULT_USER_ID)
        .badge(badge)
        .acquiredAt(LocalDateTime.now())
        .build();
  }

  public static UserBadge revoked() {
    return UserBadge.builder()
        .id(DEFAULT_USER_BADGE_ID)
        .userId(DEFAULT_USER_ID)
        .badge(BadgeFixture.basic())
        .acquiredAt(LocalDateTime.now().minusDays(10))
        .revoked(true)
        .revokedAt(LocalDateTime.now())
        .revokeReason("어뷰징")
        .build();
  }
}
