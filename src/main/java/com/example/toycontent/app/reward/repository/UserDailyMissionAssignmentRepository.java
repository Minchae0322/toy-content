package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.UserDailyMissionAssignment;
import com.example.toycontent.app.reward.repository.querydsl.UserDailyMissionAssignmentRepositoryCustom;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyMissionAssignmentRepository
    extends JpaRepository<UserDailyMissionAssignment, Long>,
    UserDailyMissionAssignmentRepositoryCustom {

  List<UserDailyMissionAssignment> findByUserIdAndAssignedDate(Long userId, LocalDate assignedDate);

  Optional<UserDailyMissionAssignment> findByUserIdAndMissionIdAndAssignedDate(
      Long userId, Long missionId, LocalDate assignedDate);

  boolean existsByUserIdAndAssignedDate(Long userId, LocalDate assignedDate);
}
