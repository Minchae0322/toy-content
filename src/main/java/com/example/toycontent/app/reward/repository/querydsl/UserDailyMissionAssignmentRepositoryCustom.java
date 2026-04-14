package com.example.toycontent.app.reward.repository.querydsl;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.MissionAssignmentInfo;
import java.time.LocalDate;
import java.util.List;

public interface UserDailyMissionAssignmentRepositoryCustom {

  List<MissionAssignmentInfo> findAssignmentsWithMissionByUserIdAndDate(Long userId,
      LocalDate date);
}
