package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.enumuration.MissionProgressStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.MissionAssignmentInfo;
import com.example.toycontent.app.reward.domain.DailyMission;
import com.example.toycontent.app.reward.domain.UserDailyMissionAssignment;
import com.example.toycontent.app.reward.repository.UserDailyMissionAssignmentRepository;
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

  private final UserDailyMissionAssignmentRepository assignmentRepository;
  private final DailyMissionService dailyMissionService;

  @Transactional
  public List<UserDailyMissionAssignment> assignDailyMissions(Long userId, LocalDate date,
      List<Long> missionIds) {
    if (assignmentRepository.existsByUserIdAndAssignedDate(userId, date)) {
      return assignmentRepository.findByUserIdAndAssignedDate(userId, date);
    }
    List<UserDailyMissionAssignment> assignments = missionIds.stream()
        .map(missionId -> {
          DailyMission mission = dailyMissionService.getMissionById(missionId);
          return UserDailyMissionAssignment.builder()
              .userId(userId)
              .mission(mission)
              .assignedDate(date)
              .targetCount(mission.getTargetCount())
              .build();
        })
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
  public UserDailyMissionAssignment claimReward(Long userId, Long assignmentId) {
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
    return assignment;
  }

  public List<MissionAssignmentInfo> getTodayAssignments(Long userId) {
    return assignmentRepository.findAssignmentsWithMissionByUserIdAndDate(
        userId, LocalDate.now());
  }

  public List<MissionAssignmentInfo> getAssignmentsByDate(Long userId, LocalDate date) {
    return assignmentRepository.findAssignmentsWithMissionByUserIdAndDate(userId, date);
  }
}
