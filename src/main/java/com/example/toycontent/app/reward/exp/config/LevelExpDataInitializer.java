package com.example.toycontent.app.reward.exp.config;

import com.example.toycontent.app.reward.exp.domain.LevelExp;
import com.example.toycontent.app.reward.exp.repository.LevelExpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 최초 기동 시 tb_level_exp 테이블에 레벨별 필요 EXP 초기 데이터를 적재한다.
 *
 * <p>레벨 테이블은 LevelExpService가 캐시하여 사용하며,
 * UserReward.totalExp → 레벨/티어 계산의 기준이 된다.</p>
 *
 * <h3>동작 방식</h3>
 * <ul>
 *   <li>테이블이 비어 있을 때만 INSERT (멱등성 보장)</li>
 *   <li>이미 데이터가 존재하면 스킵 → 운영 중 수치 변경은 DB 직접 UPDATE 후
 *       LevelExpService.reload() 호출</li>
 * </ul>
 *
 * <h3>레벨 구간별 필요 EXP 설계</h3>
 * <pre>
 * Lv 1      : 0 (시작 레벨)
 * Lv 2~5    : 100 ~ 400   (PLAIN 티어, 초반 빠른 성장)
 * Lv 6~10   : 500 ~ 900   (FRUITY 티어 진입)
 * Lv 11~15  : 1,000 ~ 1,800
 * Lv 16~20  : 2,500 ~ 4,500 (GRANOLA 티어 진입)
 * Lv 21~25  : 5,000 ~ 7,000
 * Lv 26~30  : 8,000 ~ 10,000 (PARFAIT 티어 진입)
 * Lv 31~35  : 11,000 ~ 20,000
 * Lv 36~40  : 25,000 ~ 43,000 (SIGNATURE 티어 진입, 최대 레벨)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LevelExpDataInitializer implements ApplicationRunner {

  private final LevelExpRepository levelExpRepository;

  /** 인덱스 = 레벨 - 1. 이전 레벨에서 해당 레벨로 올리는 데 필요한 EXP. */
  private static final long[] REQUIRED_EXP = {
      0,                                              // Lv 1 (시작)
      100, 200, 300, 400,                             // Lv 2~5   (PLAIN)
      500, 600, 700, 800, 900,                        // Lv 6~10  (FRUITY)
      1_000, 1_200, 1_400, 1_600, 1_800,              // Lv 11~15 (FRUITY)
      2_500, 3_000, 3_500, 4_000, 4_500,              // Lv 16~20 (GRANOLA)
      5_000, 5_500, 6_000, 6_500, 7_000,              // Lv 21~25 (GRANOLA)
      8_000, 8_500, 9_000, 9_500, 10_000,             // Lv 26~30 (PARFAIT)
      11_000, 13_000, 15_000, 17_000, 20_000,         // Lv 31~35 (PARFAIT)
      25_000, 28_000, 32_000, 37_000, 43_000          // Lv 36~40 (SIGNATURE)
  };

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (levelExpRepository.count() > 0) {
      log.info("[reward] 레벨 테이블 이미 존재 - 초기화 스킵");
      return;
    }

    long cumulative = 0;
    for (int i = 0; i < REQUIRED_EXP.length; i++) {
      int level = i + 1;
      cumulative += REQUIRED_EXP[i];
      levelExpRepository.save(LevelExp.builder()
          .level(level)
          .requiredExp(REQUIRED_EXP[i])
          .cumulativeExp(cumulative)
          .build());
    }
    log.info("[reward] 레벨 테이블 초기화 완료 - {}개 레벨", REQUIRED_EXP.length);
  }
}
