package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.PredictionInfo;
import com.example.toycontent.app.reward.domain.BattlePrediction;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.repository.BattlePredictionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattlePredictionService {

  private final BattlePredictionRepository battlePredictionRepository;
  private final ExpGrantService expGrantService;

  @Transactional
  public BattlePrediction createPrediction(Long userId, Battle battle,
      BattleItem predictedItem) {
    if (battlePredictionRepository.existsByUserIdAndBattleId(userId, battle.getId())) {
      throw new RestApiException(RewardErrorCode.PREDICTION_ALREADY_EXISTS);
    }
    BattlePrediction prediction = BattlePrediction.builder()
        .userId(userId)
        .battle(battle)
        .predictedItem(predictedItem)
        .build();
    return battlePredictionRepository.save(prediction);
  }

  @Transactional
  public List<BattlePrediction> settleBattle(Long battleId, BattleItem winnerItem) {
    List<BattlePrediction> predictions =
        battlePredictionRepository.findByBattleIdAndHitIsNull(battleId);
    predictions.forEach(prediction -> prediction.settle(winnerItem));
    long hitCount = predictions.stream().filter(BattlePrediction::getHit).count();
    log.info("배틀 예측 판정 완료 - battleId: {}, 총 {}건, 적중 {}건",
        battleId, predictions.size(), hitCount);

    // 적중한 유저에게 EXP 지급
    predictions.stream()
        .filter(BattlePrediction::getHit)
        .forEach(p -> expGrantService.grantBattlePredictionHit(p.getUserId(), p.getId()));

    return predictions;
  }

  public BattlePrediction getPrediction(Long userId, Long battleId) {
    return battlePredictionRepository.findByUserIdAndBattleId(userId, battleId)
        .orElse(null);
  }

  public List<PredictionInfo> getUserPredictionHistory(Long userId, Pageable pageable) {
    return battlePredictionRepository.findPredictionHistoryByUserIdWithDetail(userId, pageable);
  }

  public long getUserHitCount(Long userId) {
    return battlePredictionRepository.countByUserIdAndHit(userId, true);
  }

  public long getUserTotalPredictions(Long userId) {
    return battlePredictionRepository.countByUserId(userId);
  }

}
