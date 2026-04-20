package com.example.toycontent.support.fixture;

import com.example.toycontent.app.common.enumuration.MissionDifficulty;
import com.example.toycontent.app.reward.mission.domain.DailyMission;

public class DailyMissionFixture {

  public static final Long DEFAULT_MISSION_ID = 1L;
  public static final String DEFAULT_CODE = "PRESS_FIRE_5";

  private DailyMissionFixture() {}

  public static DailyMission easy() {
    return DailyMission.builder()
        .id(DEFAULT_MISSION_ID)
        .code(DEFAULT_CODE)
        .title("불꽃 5개 누르기")
        .description("피드에서 불꽃을 5개 눌러보세요")
        .difficulty(MissionDifficulty.EASY)
        .targetCount(5)
        .rewardExp(20)
        .build();
  }

  public static DailyMission hard() {
    return DailyMission.builder()
        .id(2L)
        .code("WRITE_FEED_WITH_BUY_INFO")
        .title("구매처 포함 글쓰기")
        .description("구매처 정보가 포함된 피드를 작성하세요")
        .difficulty(MissionDifficulty.HARD)
        .targetCount(1)
        .rewardExp(100)
        .grantsGachaTicket(true)
        .isFixedCandidate(true)
        .build();
  }

  public static DailyMission withCode(String code) {
    return DailyMission.builder()
        .id(DEFAULT_MISSION_ID)
        .code(code)
        .title("테스트 미션")
        .difficulty(MissionDifficulty.NORMAL)
        .targetCount(1)
        .rewardExp(50)
        .build();
  }
}
