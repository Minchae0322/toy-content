package com.example.toycontent.app.reward.exp.repository;

import com.example.toycontent.app.reward.exp.domain.UserReward;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRewardRepository extends JpaRepository<UserReward, Long> {

  Optional<UserReward> findByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT ur FROM UserReward ur WHERE ur.userId = :userId")
  Optional<UserReward> findByUserIdWithLock(@Param("userId") Long userId);
}
