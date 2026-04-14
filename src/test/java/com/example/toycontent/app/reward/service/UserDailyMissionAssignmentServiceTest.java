package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.MissionAssignmentInfo;
import com.example.toycontent.app.reward.domain.DailyMission;
import com.example.toycontent.app.reward.domain.UserDailyMissionAssignment;
import com.example.toycontent.app.reward.repository.UserDailyMissionAssignmentRepository;
import com.example.toycontent.support.fixture.DailyMissionFixture;
import com.example.toycontent.support.fixture.UserDailyMissionAssignmentFixture;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDailyMissionAssignmentService")
class UserDailyMissionAssignmentServiceTest {

  private static final Long USER_ID = 100L;

  @Mock private UserDailyMissionAssignmentRepository assignmentRepository;
  @Mock private DailyMissionService dailyMissionService;
  @InjectMocks private UserDailyMissionAssignmentService assignmentService;

  @Nested
  @DisplayName("progressMission - 미션 진행")
  class ProgressMission {

    @Test
    @DisplayName("진행 중인 미션의 카운트를 증가시킨다")
    void 카운트_증가() {
      // given
      DailyMission mission = DailyMissionFixture.easy();
      UserDailyMissionAssignment assignment = UserDailyMissionAssignmentFixture.inProgress();
      given(dailyMissionService.getMissionByCode("PRESS_FIRE_5")).willReturn(mission);
      given(assignmentRepository.findByUserIdAndMissionIdAndAssignedDate(
          eq(USER_ID), eq(mission.getId()), any(LocalDate.class)))
          .willReturn(Optional.of(assignment));

      // when
      assignmentService.progressMission(USER_ID, "PRESS_FIRE_5", 1);

      // then
      assertThat(assignment.getCurrentCount()).as("진행 카운트").isEqualTo(3);
    }

    @Test
    @DisplayName("목표 달성 시 자동으로 COMPLETED 상태가 된다")
    void 자동_완료() {
      // given
      DailyMission mission = DailyMissionFixture.easy();
      UserDailyMissionAssignment assignment = UserDailyMissionAssignmentFixture.inProgress();
      given(dailyMissionService.getMissionByCode("PRESS_FIRE_5")).willReturn(mission);
      given(assignmentRepository.findByUserIdAndMissionIdAndAssignedDate(
          eq(USER_ID), eq(mission.getId()), any(LocalDate.class)))
          .willReturn(Optional.of(assignment));

      // when
      assignmentService.progressMission(USER_ID, "PRESS_FIRE_5", 10);

      // then
      assertThat(assignment.getCurrentCount()).as("목표에 클램핑").isEqualTo(5);
    }
  }

  @Nested
  @DisplayName("claimReward - 보상 수령")
  class ClaimReward {

    @Test
    @DisplayName("완료된 미션의 보상을 정상 수령한다")
    void 정상_수령() {
      // given
      UserDailyMissionAssignment assignment = UserDailyMissionAssignmentFixture.completed();
      given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));

      // when
      UserDailyMissionAssignment result = assignmentService.claimReward(USER_ID, 1L);

      // then
      assertThat(result.getClaimedAt()).isNotNull();
    }

    @Test
    @DisplayName("미완료 미션의 보상 수령 시 RestApiException을 던진다")
    void 미완료_예외() {
      // given
      UserDailyMissionAssignment assignment = UserDailyMissionAssignmentFixture.inProgress();
      given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));

      // when & then
      assertThatThrownBy(() -> assignmentService.claimReward(USER_ID, 1L))
          .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("이미 수령한 미션의 보상 수령 시 RestApiException을 던진다")
    void 이미_수령_예외() {
      // given
      UserDailyMissionAssignment assignment = UserDailyMissionAssignmentFixture.claimed();
      given(assignmentRepository.findById(1L)).willReturn(Optional.of(assignment));

      // when & then
      assertThatThrownBy(() -> assignmentService.claimReward(USER_ID, 1L))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getTodayAssignments - 오늘 배정 미션 조회")
  class GetTodayAssignments {

    @Test
    @DisplayName("QueryDSL을 통해 오늘 배정 미션을 조회한다")
    void 정상_조회() {
      // given
      MissionAssignmentInfo info = MissionAssignmentInfo.builder()
          .id(1L)
          .currentCount(0)
          .targetCount(5)
          .build();
      given(assignmentRepository.findAssignmentsWithMissionByUserIdAndDate(
          eq(USER_ID), any(LocalDate.class)))
          .willReturn(List.of(info));

      // when
      List<MissionAssignmentInfo> result = assignmentService.getTodayAssignments(USER_ID);

      // then
      assertThat(result).hasSize(1);
    }
  }
}
