package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.UserStreak;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {

  Optional<UserStreak> findByUserId(Long userId);
}
