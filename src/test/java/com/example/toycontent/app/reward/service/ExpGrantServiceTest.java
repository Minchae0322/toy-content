package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.reward.domain.DailyExpCap;
import com.example.toycontent.app.reward.domain.UserReward;
import com.example.toycontent.app.reward.repository.DailyExpCapRepository;
import com.example.toycontent.app.reward.repository.ExpHistoryRepository;
import com.example.toycontent.app.reward.service.dto.ExpGrantResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpGrantService")
class ExpGrantServiceTest {

  private static final Long USER_ID = 100L;
  private static final Long FEED_ID = 1L;
  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

  @Mock private UserRewardService userRewardService;
  @Mock private ExpHistoryRepository expHistoryRepository;
  @Mock private DailyExpCapRepository dailyExpCapRepository;
  @InjectMocks private ExpGrantService expGrantService;

  private DailyExpCap freshCap() {
    return DailyExpCap.builder()
        .userId(USER_ID)
        .capDate(TODAY)
        .build();
  }

  private DailyExpCap usedCap(long used) {
    DailyExpCap cap = freshCap();
    cap.consume(used, 200);
    return cap;
  }

  private void givenNoDuplicate() {
    given(expHistoryRepository.existsByUserIdAndSourceAndSourceId(
        any(), any(), any())).willReturn(false);
  }

  private void givenFreshCap() {
    given(dailyExpCapRepository.findByUserIdAndCapDate(eq(USER_ID), any()))
        .willReturn(Optional.of(freshCap()));
  }

  @Nested
  @DisplayName("grantFeedCreate - 피드 작성 EXP")
  class GrantFeedCreate {

    @Test
    @DisplayName("정상 지급 시 20 EXP가 부여된다")
    void 정상_지급() {
      givenNoDuplicate();
      givenFreshCap();
      given(userRewardService.addExp(eq(USER_ID), eq(20L), eq(ExpSource.FEED_CREATE), eq(FEED_ID)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(20L).build());

      ExpGrantResult result = expGrantService.grantFeedCreate(USER_ID, FEED_ID);

      assertSoftly(softly -> {
        softly.assertThat(result.granted()).as("지급 여부").isTrue();
        softly.assertThat(result.actualAmount()).as("실제 지급 EXP").isEqualTo(20);
        softly.assertThat(result.capped()).as("캡 여부").isFalse();
        softly.assertThat(result.duplicate()).as("중복 여부").isFalse();
      });
    }

    @Test
    @DisplayName("같은 피드에 중복 지급 시 건너뛴다")
    void 중복_지급_방지() {
      given(expHistoryRepository.existsByUserIdAndSourceAndSourceId(
          USER_ID, ExpSource.FEED_CREATE, FEED_ID)).willReturn(true);

      ExpGrantResult result = expGrantService.grantFeedCreate(USER_ID, FEED_ID);

      assertSoftly(softly -> {
        softly.assertThat(result.granted()).as("지급 여부").isFalse();
        softly.assertThat(result.duplicate()).as("중복 여부").isTrue();
      });
      verify(userRewardService, never()).addExp(anyLong(), anyLong(), any(), any());
    }
  }

  @Nested
  @DisplayName("일일 캡 적용")
  class DailyCap {

    @Test
    @DisplayName("캡에 걸리면 남은 만큼만 지급된다")
    void 캡_부분_지급() {
      givenNoDuplicate();
      given(dailyExpCapRepository.findByUserIdAndCapDate(eq(USER_ID), any()))
          .willReturn(Optional.of(usedCap(190)));
      given(userRewardService.addExp(eq(USER_ID), eq(10L), eq(ExpSource.FEED_CREATE), eq(FEED_ID)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(10L).build());

      ExpGrantResult result = expGrantService.grantFeedCreate(USER_ID, FEED_ID);

      assertSoftly(softly -> {
        softly.assertThat(result.granted()).as("지급 여부").isTrue();
        softly.assertThat(result.requestedAmount()).as("요청 EXP").isEqualTo(20);
        softly.assertThat(result.actualAmount()).as("실제 지급 EXP").isEqualTo(10);
        softly.assertThat(result.capped()).as("캡 여부").isTrue();
      });
    }

    @Test
    @DisplayName("캡이 소진되면 지급하지 않는다")
    void 캡_소진() {
      givenNoDuplicate();
      given(dailyExpCapRepository.findByUserIdAndCapDate(eq(USER_ID), any()))
          .willReturn(Optional.of(usedCap(200)));

      ExpGrantResult result = expGrantService.grantFeedCreate(USER_ID, FEED_ID);

      assertSoftly(softly -> {
        softly.assertThat(result.granted()).as("지급 여부").isFalse();
        softly.assertThat(result.capped()).as("캡 여부").isTrue();
      });
      verify(userRewardService, never()).addExp(anyLong(), anyLong(), any(), any());
    }
  }

  @Nested
  @DisplayName("grantStreakBonus - 스트릭 보너스 (캡 제외)")
  class GrantStreakBonus {

    @Test
    @DisplayName("3일 마일스톤 시 20 EXP 지급 (캡 무관)")
    void 마일스톤_3일() {
      given(expHistoryRepository.existsByUserIdAndSourceAndSourceId(
          USER_ID, ExpSource.STREAK_BONUS, 3L)).willReturn(false);
      given(userRewardService.addExp(eq(USER_ID), eq(20L), eq(ExpSource.STREAK_BONUS), eq(3L)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(20L).build());

      ExpGrantResult result = expGrantService.grantStreakBonus(USER_ID, 3);

      assertSoftly(softly -> {
        softly.assertThat(result.granted()).as("지급 여부").isTrue();
        softly.assertThat(result.actualAmount()).as("실제 지급 EXP").isEqualTo(20);
        softly.assertThat(result.capped()).as("캡 여부").isFalse();
      });
    }

    @Test
    @DisplayName("100일 마일스톤 시 500 EXP 지급")
    void 마일스톤_100일() {
      given(expHistoryRepository.existsByUserIdAndSourceAndSourceId(
          USER_ID, ExpSource.STREAK_BONUS, 100L)).willReturn(false);
      given(userRewardService.addExp(eq(USER_ID), eq(500L), eq(ExpSource.STREAK_BONUS), eq(100L)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(500L).build());

      ExpGrantResult result = expGrantService.grantStreakBonus(USER_ID, 100);

      assertThat(result.actualAmount()).as("실제 지급 EXP").isEqualTo(500);
    }

    @Test
    @DisplayName("마일스톤이 아닌 일수면 지급하지 않는다")
    void 비마일스톤() {
      ExpGrantResult result = expGrantService.grantStreakBonus(USER_ID, 5);

      assertThat(result.granted()).as("지급 여부").isFalse();
    }
  }

  @Nested
  @DisplayName("grantMissionClaim - 미션 보상 (캡 제외)")
  class GrantMissionClaim {

    @Test
    @DisplayName("미션 보상은 캡에 영향받지 않는다")
    void 캡_제외() {
      Long assignmentId = 10L;
      given(expHistoryRepository.existsByUserIdAndSourceAndSourceId(
          USER_ID, ExpSource.MISSION_CLAIM, assignmentId)).willReturn(false);
      given(userRewardService.addExp(eq(USER_ID), eq(30L), eq(ExpSource.MISSION_CLAIM), eq(assignmentId)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(30L).build());

      ExpGrantResult result = expGrantService.grantMissionClaim(USER_ID, assignmentId, 30);

      assertSoftly(softly -> {
        softly.assertThat(result.granted()).as("지급 여부").isTrue();
        softly.assertThat(result.actualAmount()).as("실제 지급 EXP").isEqualTo(30);
        softly.assertThat(result.capped()).as("캡 여부").isFalse();
      });
    }
  }

  @Nested
  @DisplayName("grantFeedQualityBonus - 완성도 보너스")
  class GrantFeedQualityBonus {

    @Test
    @DisplayName("완성도 3이면 10 EXP 보너스")
    void 완성도_3() {
      givenNoDuplicate();
      givenFreshCap();
      given(userRewardService.addExp(eq(USER_ID), eq(10L), eq(ExpSource.FEED_CREATE), eq(FEED_ID)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(10L).build());

      ExpGrantResult result = expGrantService.grantFeedQualityBonus(USER_ID, FEED_ID, 3);

      assertThat(result.actualAmount()).as("보너스 EXP").isEqualTo(10);
    }

    @Test
    @DisplayName("완성도 5이면 30 EXP 보너스")
    void 완성도_5() {
      givenNoDuplicate();
      givenFreshCap();
      given(userRewardService.addExp(eq(USER_ID), eq(30L), eq(ExpSource.FEED_CREATE), eq(FEED_ID)))
          .willReturn(UserReward.builder().userId(USER_ID).totalExp(30L).build());

      ExpGrantResult result = expGrantService.grantFeedQualityBonus(USER_ID, FEED_ID, 5);

      assertThat(result.actualAmount()).as("보너스 EXP").isEqualTo(30);
    }

    @Test
    @DisplayName("완성도 2 이하면 보너스 없음")
    void 완성도_부족() {
      ExpGrantResult result = expGrantService.grantFeedQualityBonus(USER_ID, FEED_ID, 2);

      assertThat(result.granted()).as("지급 여부").isFalse();
    }
  }
}
