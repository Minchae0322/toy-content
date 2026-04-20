package com.example.toycontent.app.reward.mission.service;

import com.example.toycontent.app.common.enumuration.MissionDifficulty;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardRequest;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.DailyMissionInfo;
import com.example.toycontent.app.reward.mission.domain.DailyMission;
import com.example.toycontent.app.reward.mission.repository.DailyMissionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyMissionService {

  private final DailyMissionRepository dailyMissionRepository;

  @Transactional
  public DailyMissionInfo createMission(RewardRequest.CreateMission request) {
    if (dailyMissionRepository.existsByCode(request.getCode())) {
      throw new RestApiException(RewardErrorCode.DAILY_MISSION_CODE_DUPLICATED);
    }
    return DailyMissionInfo.from(dailyMissionRepository.save(request.toEntity()));
  }

  @Transactional
  public DailyMissionInfo updateMission(Long missionId, RewardRequest.UpdateMission request) {
    DailyMission mission = getMissionById(missionId);
    mission.update(request.getTitle(), request.getDescription(),
        request.getTargetCount(), request.getRewardExp());
    return DailyMissionInfo.from(mission);
  }

  @Transactional
  public void deactivateMission(Long missionId) {
    DailyMission mission = getMissionById(missionId);
    mission.deactivate();
  }

  public DailyMission getMissionById(Long missionId) {
    return dailyMissionRepository.findById(missionId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.DAILY_MISSION_NOT_FOUND));
  }

  public DailyMission getMissionByCode(String code) {
    return dailyMissionRepository.findByCode(code)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.DAILY_MISSION_NOT_FOUND));
  }

  public List<DailyMissionInfo> getActiveMissions() {
    return dailyMissionRepository.findByActivatedTrue().stream()
        .map(DailyMissionInfo::from)
        .toList();
  }

  public List<DailyMissionInfo> getMissionsByDifficulty(MissionDifficulty difficulty) {
    return dailyMissionRepository.findByDifficultyAndActivatedTrue(difficulty).stream()
        .map(DailyMissionInfo::from)
        .toList();
  }

  public List<DailyMissionInfo> getFixedCandidateMissions() {
    return dailyMissionRepository.findByIsFixedCandidateTrueAndActivatedTrue().stream()
        .map(DailyMissionInfo::from)
        .toList();
  }
}
