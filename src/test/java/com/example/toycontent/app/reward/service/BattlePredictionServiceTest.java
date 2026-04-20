package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.PredictionInfo;
import com.example.toycontent.app.reward.domain.BattlePrediction;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.repository.BattlePredictionRepository;
import com.example.toycontent.support.fixture.BattleFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattlePredictionService")
class BattlePredictionServiceTest {

  private static final Long USER_ID = 100L;

  @Mock private BattlePredictionRepository battlePredictionRepository;
  @Mock private ExpGrantService expGrantService;
  @InjectMocks private BattlePredictionService battlePredictionService;

  @Nested
  @DisplayName("createPrediction - 예측 생성")
  class CreatePrediction {

    @Test
    @DisplayName("첫 예측이면 정상 생성한다")
    void 정상_생성() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItem.builder().id(10L).build();
      given(battlePredictionRepository.existsByUserIdAndBattleId(USER_ID, battle.getId()))
          .willReturn(false);
      given(battlePredictionRepository.save(any(BattlePrediction.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      BattlePrediction result = battlePredictionService.createPrediction(USER_ID, battle, item);

      // then
      assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("이미 예측했으면 RestApiException을 던진다")
    void 중복_예측_예외() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItem.builder().id(10L).build();
      given(battlePredictionRepository.existsByUserIdAndBattleId(USER_ID, battle.getId()))
          .willReturn(true);

      // when & then
      assertThatThrownBy(() -> battlePredictionService.createPrediction(USER_ID, battle, item))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("settleBattle - 배틀 판정")
  class SettleBattle {

    @Test
    @DisplayName("미판정 예측들에 대해 적중 여부를 판정한다")
    void 판정_실행() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem predictedItem = BattleItem.builder().id(10L).build();
      BattleItem winnerItem = BattleItem.builder().id(10L).build();
      BattlePrediction prediction = BattlePrediction.builder()
          .id(1L).userId(USER_ID).battle(battle).predictedItem(predictedItem).build();
      given(battlePredictionRepository.findByBattleIdAndHitIsNull(battle.getId()))
          .willReturn(List.of(prediction));

      // when
      List<BattlePrediction> results = battlePredictionService.settleBattle(
          battle.getId(), winnerItem);

      // then
      assertThat(results).hasSize(1);
      assertThat(results.get(0).getHit()).as("적중").isTrue();
    }
  }

  @Nested
  @DisplayName("getUserPredictionHistory - 예측 이력 조회")
  class GetUserPredictionHistory {

    @Test
    @DisplayName("QueryDSL을 통해 예측 이력을 조회한다")
    void 정상_조회() {
      // given
      PredictionInfo info = PredictionInfo.builder().id(1L).battleId(10L).build();
      given(battlePredictionRepository.findPredictionHistoryByUserIdWithDetail(
          USER_ID, PageRequest.of(0, 10)))
          .willReturn(List.of(info));

      // when
      List<PredictionInfo> result = battlePredictionService.getUserPredictionHistory(
          USER_ID, PageRequest.of(0, 10));

      // then
      assertThat(result).hasSize(1);
    }
  }
}
