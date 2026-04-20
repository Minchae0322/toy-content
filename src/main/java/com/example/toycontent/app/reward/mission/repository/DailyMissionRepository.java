package com.example.toycontent.app.reward.mission.repository;

import com.example.toycontent.app.common.enumuration.MissionDifficulty;
import com.example.toycontent.app.reward.mission.domain.DailyMission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {

  Optional<DailyMission> findByCode(String code);

  boolean existsByCode(String code);

  List<DailyMission> findByActivatedTrue();

  List<DailyMission> findByDifficultyAndActivatedTrue(MissionDifficulty difficulty);

  List<DailyMission> findByIsFixedCandidateTrueAndActivatedTrue();
}
