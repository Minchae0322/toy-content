package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.domain.UserReward;
import com.example.toycontent.app.reward.repository.UserRewardRepository;
import com.example.toycontent.support.fixture.UserRewardFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRewardService")
class UserRewardServiceTest {

  private static final Long USER_ID = 100L;

  @Mock private UserRewardRepository userRewardRepository;
  @InjectMocks private UserRewardService userRewardService;

  @Nested
  @DisplayName("addExp - EXP 추가")
  class AddExp {

    @Test
    @DisplayName("기존 유저에게 EXP를 추가하면 totalExp가 증가한다")
    void 기존_유저_EXP_추가() {
      // given
      UserReward reward = UserRewardFixture.fresh();
      given(userRewardRepository.findByUserIdWithLock(USER_ID))
          .willReturn(Optional.of(reward));

      // when
      UserReward result = userRewardService.addExp(USER_ID, 50);

      // then
      assertThat(result.getTotalExp()).as("총 EXP").isEqualTo(50L);
    }

    @Test
    @DisplayName("첫 유저면 UserReward를 생성한 후 EXP를 추가한다")
    void 신규_유저_EXP_추가() {
      // given
      given(userRewardRepository.findByUserIdWithLock(USER_ID))
          .willReturn(Optional.empty());
      given(userRewardRepository.save(any(UserReward.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      UserReward result = userRewardService.addExp(USER_ID, 30);

      // then
      assertSoftly(softly -> {
        softly.assertThat(result.getUserId()).as("유저 ID").isEqualTo(USER_ID);
        softly.assertThat(result.getTotalExp()).as("총 EXP").isEqualTo(30L);
      });
    }

    @Test
    @DisplayName("EXP가 다음 레벨 기준에 도달하면 레벨업이 발생한다")
    void 레벨업_발생() {
      // given
      UserReward reward = UserRewardFixture.aboutToLevelUp();
      given(userRewardRepository.findByUserIdWithLock(USER_ID))
          .willReturn(Optional.of(reward));

      // when
      UserReward result = userRewardService.addExp(USER_ID, 20);

      // then
      assertThat(result.getLevel()).as("레벨업 후 레벨").isEqualTo(2);
    }

    @Test
    @DisplayName("0 이하의 EXP를 추가하면 RestApiException을 던진다")
    void 잘못된_EXP_예외() {
      // when & then
      assertThatThrownBy(() -> userRewardService.addExp(USER_ID, 0))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getUserReward - 유저 보상 조회")
  class GetUserReward {

    @Test
    @DisplayName("존재하지 않는 유저 조회 시 RestApiException을 던진다")
    void 조회_실패() {
      // given
      given(userRewardRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userRewardService.getUserReward(USER_ID))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getUserLevel - 레벨 조회")
  class GetUserLevel {

    @Test
    @DisplayName("보상 정보가 없으면 기본 레벨 1을 반환한다")
    void 기본_레벨() {
      // given
      given(userRewardRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

      // when
      int level = userRewardService.getUserLevel(USER_ID);

      // then
      assertThat(level).isEqualTo(1);
    }
  }
}
