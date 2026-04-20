package com.example.toycontent.app.reward.mission.repository;

import com.example.toycontent.app.reward.mission.domain.UserStreak;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {

  Optional<UserStreak> findByUserId(Long userId);
}
