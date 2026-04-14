package com.example.toycontent.support.fixture;

import com.example.toycontent.app.reward.domain.Badge;

public class BadgeFixture {

  public static final Long DEFAULT_BADGE_ID = 1L;
  public static final String DEFAULT_CODE = "BUY_PLACE_SHARER";

  private BadgeFixture() {}

  public static Badge basic() {
    return Badge.builder()
        .id(DEFAULT_BADGE_ID)
        .code(DEFAULT_CODE)
        .name("구매처 쉐어러")
        .description("구매처 정보를 공유한 유저")
        .iconEmoji("\uD83C\uDFEA")
        .category("BRAG")
        .build();
  }

  public static Badge withCode(String code) {
    return Badge.builder()
        .id(DEFAULT_BADGE_ID)
        .code(code)
        .name("테스트 뱃지")
        .category("BRAG")
        .build();
  }

  public static Badge seasonal(String seasonCode) {
    return Badge.builder()
        .id(DEFAULT_BADGE_ID)
        .code("SEASON_BADGE")
        .name("시즌 뱃지")
        .isSeasonal(true)
        .seasonCode(seasonCode)
        .category("SEASON")
        .build();
  }
}
