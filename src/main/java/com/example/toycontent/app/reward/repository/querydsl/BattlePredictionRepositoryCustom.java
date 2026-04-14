package com.example.toycontent.app.reward.repository.querydsl;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.PredictionInfo;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface BattlePredictionRepositoryCustom {

  List<PredictionInfo> findPredictionHistoryByUserIdWithDetail(Long userId, Pageable pageable);

  Long countPredictionHistory(Long userId);
}
