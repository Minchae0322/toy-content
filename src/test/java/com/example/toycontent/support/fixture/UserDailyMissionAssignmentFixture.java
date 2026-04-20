package com.example.toycontent.support.fixture;

import com.example.toycontent.app.common.enumuration.MissionProgressStatus;
import com.example.toycontent.app.reward.mission.domain.DailyMission;
import com.example.toycontent.app.reward.mission.domain.UserDailyMissionAssignment;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDailyMissionAssignmentFixture {

  public static final Long DEFAULT_USER_ID = 100L;
  public static final Long DEFAULT_ASSIGNMENT_ID = 1L;

  private UserDailyMissionAssignmentFixture() {}

  public static UserDailyMissionAssignment inProgress() {
    return UserDailyMissionAssignment.builder()
        .id(DEFAULT_ASSIGNMENT_ID)
        .userId(DEFAULT_USER_ID)
        .mission(DailyMissionFixture.easy())
        .assignedDate(LocalDate.now())
        .currentCount(2)
        .targetCount(5)
        .status(MissionProgressStatus.IN_PROGRESS)
        .build();
  }

  public static UserDailyMissionAssignment completed() {
    return UserDailyMissionAssignment.builder()
        .id(DEFAULT_ASSIGNMENT_ID)
        .userId(DEFAULT_USER_ID)
        .mission(DailyMissionFixture.easy())
        .assignedDate(LocalDate.now())
        .currentCount(5)
        .targetCount(5)
        .status(MissionProgressStatus.COMPLETED)
        .completedAt(LocalDateTime.now())
        .build();
  }

  public static UserDailyMissionAssignment claimed() {
    return UserDailyMissionAssignment.builder()
        .id(DEFAULT_ASSIGNMENT_ID)
        .userId(DEFAULT_USER_ID)
        .mission(DailyMissionFixture.easy())
        .assignedDate(LocalDate.now())
        .currentCount(5)
        .targetCount(5)
        .status(MissionProgressStatus.CLAIMED)
        .completedAt(LocalDateTime.now().minusMinutes(10))
        .claimedAt(LocalDateTime.now())
        .build();
  }

  public static UserDailyMissionAssignment withMission(DailyMission mission) {
    return UserDailyMissionAssignment.builder()
        .id(DEFAULT_ASSIGNMENT_ID)
        .userId(DEFAULT_USER_ID)
        .mission(mission)
        .assignedDate(LocalDate.now())
        .currentCount(0)
        .targetCount(mission.getTargetCount())
        .status(MissionProgressStatus.IN_PROGRESS)
        .build();
  }
}
