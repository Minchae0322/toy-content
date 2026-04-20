package com.example.toycontent.app.reward.mission.service;

import com.example.toycontent.app.common.enumuration.MissionProgressStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.MissionAssignmentInfo;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantInfo;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantResult;
import com.example.toycontent.app.reward.mission.domain.DailyMission;
import com.example.toycontent.app.reward.mission.domain.UserDailyMissionAssignment;
import com.example.toycontent.app.reward.mission.repository.UserDailyMissionAssignmentRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDailyMissionAssignmentService {

  public record ClaimResult(UserDailyMissionAssignment assignment, ExpGrantInfo expGrant) {}

  private final UserDailyMissionAssignmentRepository assignmentRepository;
  private final DailyMissionService dailyMissionService;
  private final ExpGrantService expGrantService;

  @Transactional
  public List<UserDailyMissionAssignment> assignDailyMissions(Long userId, LocalDate date,
      List<Long> missionIds) {
    if (assignmentRepository.existsByUserIdAndAssignedDate(userId, date)) {
      return assignmentRepository.findByUserIdAndAssignedDate(userId, date);
    }
    List<UserDailyMissionAssignment> assignments = missionIds.stream()
        .map(missionId -> createAssignment(userId, dailyMissionService.getMissionById(missionId), date))
        .toList();
    return assignmentRepository.saveAll(assignments);
  }

  @Transactional
  public void progressMission(Long userId, String missionCode, int incrementBy) {
    LocalDate today = LocalDate.now();
    DailyMission mission = dailyMissionService.getMissionByCode(missionCode);
    assignmentRepository
        .findByUserIdAndMissionIdAndAssignedDate(userId, mission.getId(), today)
        .ifPresent(assignment -> {
          boolean completed = assignment.incrementProgress(incrementBy);
          if (completed) {
            log.info("미션 완료 - userId: {}, missionCode: {}", userId, missionCode);
          }
        });
  }

  @Transactional
  public ClaimResult claimReward(Long userId, Long assignmentId) {
    UserDailyMissionAssignment assignment = assignmentRepository.findById(assignmentId)
        .filter(a -> a.getUserId().equals(userId))
        .orElseThrow(() -> new RestApiException(RewardErrorCode.MISSION_ASSIGNMENT_NOT_FOUND));

    if (assignment.getStatus() == MissionProgressStatus.CLAIMED) {
      throw new RestApiException(RewardErrorCode.MISSION_ALREADY_CLAIMED);
    }
    if (assignment.getStatus() != MissionProgressStatus.COMPLETED) {
      throw new RestApiException(RewardErrorCode.MISSION_NOT_COMPLETED);
    }
    assignment.claim();

    // 미션 보상 EXP 지급 (캡 제외)
    ExpGrantResult grant = expGrantService.grantMissionClaim(
        userId, assignmentId, assignment.getMission().getRewardExp());

    return new ClaimResult(assignment, ExpGrantInfo.aggregate(grant));
  }

  public List<MissionAssignmentInfo> getTodayAssignments(Long userId) {
    return assignmentRepository.findAssignmentsWithMissionByUserIdAndDate(
        userId, LocalDate.now());
  }

  public List<MissionAssignmentInfo> getAssignmentsByDate(Long userId, LocalDate date) {
    return assignmentRepository.findAssignmentsWithMissionByUserIdAndDate(userId, date);
  }

  private UserDailyMissionAssignment createAssignment(Long userId, DailyMission mission, LocalDate date) {
    return UserDailyMissionAssignment.builder()
        .userId(userId)
        .mission(mission)
        .assignedDate(date)
        .targetCount(mission.getTargetCount())
        .build();
  }
}
