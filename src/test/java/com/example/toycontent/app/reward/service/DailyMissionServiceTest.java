package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.controller.dto.RewardRequest;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.DailyMissionInfo;
import com.example.toycontent.app.reward.domain.DailyMission;
import com.example.toycontent.app.reward.repository.DailyMissionRepository;
import com.example.toycontent.support.fixture.DailyMissionFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyMissionService")
class DailyMissionServiceTest {

  @Mock private DailyMissionRepository dailyMissionRepository;
  @InjectMocks private DailyMissionService dailyMissionService;

  @Nested
  @DisplayName("createMission - 미션 생성")
  class CreateMission {

    @Test
    @DisplayName("코드가 중복되지 않으면 미션을 정상 생성한다")
    void 정상_생성() {
      // given
      RewardRequest.CreateMission request = RewardRequest.CreateMission.builder()
          .code("NEW_MISSION")
          .title("새 미션")
          .description("설명")
          .difficulty("EASY")
          .targetCount(5)
          .rewardExp(20)
          .build();
      given(dailyMissionRepository.existsByCode("NEW_MISSION")).willReturn(false);
      given(dailyMissionRepository.save(any(DailyMission.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      DailyMissionInfo result = dailyMissionService.createMission(request);

      // then
      assertThat(result.getCode()).isEqualTo("NEW_MISSION");
    }

    @Test
    @DisplayName("코드가 중복되면 RestApiException을 던진다")
    void 코드_중복_예외() {
      // given
      RewardRequest.CreateMission request = RewardRequest.CreateMission.builder()
          .code("EXISTING")
          .title("미션")
          .difficulty("EASY")
          .targetCount(1)
          .rewardExp(10)
          .build();
      given(dailyMissionRepository.existsByCode("EXISTING")).willReturn(true);

      // when & then
      assertThatThrownBy(() -> dailyMissionService.createMission(request))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getMissionById - 미션 조회")
  class GetMissionById {

    @Test
    @DisplayName("존재하는 미션을 정상 조회한다")
    void 정상_조회() {
      // given
      DailyMission mission = DailyMissionFixture.easy();
      given(dailyMissionRepository.findById(1L)).willReturn(Optional.of(mission));

      // when
      DailyMission result = dailyMissionService.getMissionById(1L);

      // then
      assertThat(result.getCode()).isEqualTo(DailyMissionFixture.DEFAULT_CODE);
    }

    @Test
    @DisplayName("존재하지 않는 미션 조회 시 RestApiException을 던진다")
    void 조회_실패() {
      // given
      given(dailyMissionRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> dailyMissionService.getMissionById(999L))
          .isInstanceOf(RestApiException.class);
    }
  }
}
