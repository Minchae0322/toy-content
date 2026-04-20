package com.example.toycontent.app.reward.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.mission.domain.UserStreak;
import com.example.toycontent.app.reward.mission.repository.UserStreakRepository;
import com.example.toycontent.support.fixture.UserStreakFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserStreakService")
class UserStreakServiceTest {

  private static final Long USER_ID = 100L;

  @Mock private UserStreakRepository userStreakRepository;
  @Mock private ExpGrantService expGrantService;
  @InjectMocks private UserStreakService userStreakService;

  @Nested
  @DisplayName("recordPosting - 인증 작성 기록")
  class RecordPosting {

    @Test
    @DisplayName("연속 작성 시 currentStreak이 증가한다")
    void 연속_작성() {
      // given
      UserStreak streak = UserStreakFixture.withStreak(3);
      given(userStreakRepository.findByUserId(USER_ID)).willReturn(Optional.of(streak));

      // when
      UserStreak result = userStreakService.recordPosting(USER_ID).streak();

      // then
      assertThat(result.getCurrentStreak()).as("스트릭 증가").isEqualTo(4);
    }

    @Test
    @DisplayName("첫 유저면 UserStreak을 생성 후 기록한다")
    void 신규_유저() {
      // given
      given(userStreakRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
      given(userStreakRepository.save(any(UserStreak.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      UserStreak result = userStreakService.recordPosting(USER_ID).streak();

      // then
      assertThat(result.getCurrentStreak()).as("첫 스트릭").isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("useRecoveryTicket - 복구 티켓 사용")
  class UseRecoveryTicket {

    @Test
    @DisplayName("복구 티켓이 있으면 스트릭을 복구한다")
    void 정상_복구() {
      // given
      UserStreak streak = UserStreakFixture.withRecoveryTickets(2);
      given(userStreakRepository.findByUserId(USER_ID)).willReturn(Optional.of(streak));

      // when
      UserStreak result = userStreakService.useRecoveryTicket(USER_ID);

      // then
      assertThat(result.getRecoveryTickets()).as("남은 티켓").isEqualTo(1);
    }

    @Test
    @DisplayName("복구 티켓이 없으면 RestApiException을 던진다")
    void 티켓_없음_예외() {
      // given
      UserStreak streak = UserStreakFixture.fresh();
      given(userStreakRepository.findByUserId(USER_ID)).willReturn(Optional.of(streak));

      // when & then
      assertThatThrownBy(() -> userStreakService.useRecoveryTicket(USER_ID))
          .isInstanceOf(RestApiException.class);
    }
  }
}
