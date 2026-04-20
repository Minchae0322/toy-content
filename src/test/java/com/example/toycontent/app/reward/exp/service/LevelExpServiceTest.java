package com.example.toycontent.app.reward.exp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;

import com.example.toycontent.app.common.enumuration.UserTier;
import com.example.toycontent.app.reward.exp.service.dto.LevelInfo;
import com.example.toycontent.app.reward.exp.domain.LevelExp;
import com.example.toycontent.app.reward.exp.repository.LevelExpRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevelExpService")
class LevelExpServiceTest {

  @Mock private LevelExpRepository levelExpRepository;
  @InjectMocks private LevelExpService levelExpService;

  private static final List<LevelExp> LEVEL_TABLE = buildLevelTable();

  private static List<LevelExp> buildLevelTable() {
    long[] requiredExp = {
        0, 100, 200, 300, 400,
        500, 600, 700, 800, 900,
        1_000, 1_200, 1_400, 1_600, 1_800,
        2_500, 3_000, 3_500, 4_000, 4_500,
        5_000, 5_500, 6_000, 6_500, 7_000,
        8_000, 8_500, 9_000, 9_500, 10_000,
        11_000, 13_000, 15_000, 17_000, 20_000,
        25_000, 28_000, 32_000, 37_000, 43_000
    };
    long cumulative = 0;
    List<LevelExp> table = new java.util.ArrayList<>();
    for (int i = 0; i < requiredExp.length; i++) {
      cumulative += requiredExp[i];
      table.add(LevelExp.builder()
          .level(i + 1)
          .requiredExp(requiredExp[i])
          .cumulativeExp(cumulative)
          .build());
    }
    return List.copyOf(table);
  }

  @BeforeEach
  void setUp() {
    given(levelExpRepository.findAllByOrderByLevelAsc()).willReturn(LEVEL_TABLE);
  }

  @Nested
  @DisplayName("computeLevelInfo - 레벨 계산")
  class ComputeLevelInfo {

    @Test
    @DisplayName("EXP 0이면 레벨 1, PLAIN 티어")
    void EXP_0_레벨1() {
      LevelInfo info = levelExpService.computeLevelInfo(0);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(1);
        softly.assertThat(info.tier()).as("티어").isEqualTo(UserTier.PLAIN);
        softly.assertThat(info.currentLevelExp()).as("현재 레벨 EXP").isEqualTo(0L);
        softly.assertThat(info.maxLevel()).as("최대 레벨").isFalse();
      });
    }

    @Test
    @DisplayName("EXP 99이면 레벨 1, 다음 레벨까지 1 필요")
    void 레벨1_경계_직전() {
      LevelInfo info = levelExpService.computeLevelInfo(99);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(1);
        softly.assertThat(info.currentLevelExp()).as("현재 레벨 EXP").isEqualTo(99L);
        softly.assertThat(info.nextLevelExp()).as("다음 레벨까지").isEqualTo(1L);
      });
    }

    @Test
    @DisplayName("EXP 100이면 정확히 레벨 2")
    void 레벨2_정확히() {
      LevelInfo info = levelExpService.computeLevelInfo(100);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(2);
        softly.assertThat(info.currentLevelExp()).as("현재 레벨 EXP").isEqualTo(0L);
        softly.assertThat(info.tier()).as("티어").isEqualTo(UserTier.PLAIN);
      });
    }

    @Test
    @DisplayName("EXP 1500이면 레벨 6, FRUITY 티어 시작")
    void FRUITY_티어_시작() {
      LevelInfo info = levelExpService.computeLevelInfo(1500);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(6);
        softly.assertThat(info.tier()).as("티어").isEqualTo(UserTier.FRUITY);
        softly.assertThat(info.currentLevelExp()).as("현재 레벨 EXP").isEqualTo(0L);
      });
    }

    @Test
    @DisplayName("EXP 14000이면 레벨 16, GRANOLA 티어 시작")
    void GRANOLA_티어_시작() {
      LevelInfo info = levelExpService.computeLevelInfo(14_000);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(16);
        softly.assertThat(info.tier()).as("티어").isEqualTo(UserTier.GRANOLA);
      });
    }

    @Test
    @DisplayName("EXP 중간값에서 currentLevelExp와 nextLevelExp가 올바르게 계산된다")
    void 중간_EXP_계산() {
      LevelInfo info = levelExpService.computeLevelInfo(150);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(2);
        softly.assertThat(info.currentLevelExp()).as("현재 레벨 EXP").isEqualTo(50L);
        softly.assertThat(info.nextLevelExp()).as("다음 레벨까지").isEqualTo(150L);
      });
    }
  }

  @Nested
  @DisplayName("최대 레벨")
  class MaxLevel {

    @Test
    @DisplayName("최대 레벨 도달 시 maxLevel이 true")
    void 최대_레벨_도달() {
      LevelInfo info = levelExpService.computeLevelInfo(345_000);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(40);
        softly.assertThat(info.tier()).as("티어").isEqualTo(UserTier.SIGNATURE);
        softly.assertThat(info.maxLevel()).as("최대 레벨").isTrue();
        softly.assertThat(info.nextLevelExp()).as("다음 레벨까지").isEqualTo(0L);
      });
    }

    @Test
    @DisplayName("최대 레벨 초과 EXP도 정상 처리")
    void 최대_레벨_초과() {
      LevelInfo info = levelExpService.computeLevelInfo(500_000);

      assertSoftly(softly -> {
        softly.assertThat(info.level()).as("레벨").isEqualTo(40);
        softly.assertThat(info.maxLevel()).as("최대 레벨").isTrue();
        softly.assertThat(info.currentLevelExp()).as("초과 EXP").isEqualTo(155_000L);
      });
    }

  }
}
