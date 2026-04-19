package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.DailyExpCap;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyExpCapRepository extends JpaRepository<DailyExpCap, Long> {

  Optional<DailyExpCap> findByUserIdAndCapDate(Long userId, LocalDate capDate);
}
