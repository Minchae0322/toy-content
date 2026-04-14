package com.example.toycontent.app.reward.repository;

import com.example.toycontent.app.reward.domain.BattlePrediction;
import com.example.toycontent.app.reward.repository.querydsl.BattlePredictionRepositoryCustom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattlePredictionRepository extends JpaRepository<BattlePrediction, Long>,
    BattlePredictionRepositoryCustom {

  Optional<BattlePrediction> findByUserIdAndBattleId(Long userId, Long battleId);

  boolean existsByUserIdAndBattleId(Long userId, Long battleId);

  List<BattlePrediction> findByBattleIdAndHitIsNull(Long battleId);

  long countByUserIdAndHit(Long userId, boolean hit);

  long countByUserId(Long userId);
}
