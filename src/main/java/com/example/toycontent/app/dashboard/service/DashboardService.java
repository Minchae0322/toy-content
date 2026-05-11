package com.example.toycontent.app.dashboard.service;

import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.dashboard.controller.dto.DashboardSummaryResponse;
import com.example.toycontent.app.feed.repository.FeedRepository;
import java.time.LocalDateTime;

import com.example.toycontent.app.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

  private final BattleRepository battleRepository;
  private final ProductRepository productRepository;
  private final FeedRepository feedRepository;

  /** 인기 피드 판별 기준 핫스코어 임계값 */
  private static final double HOT_SCORE_THRESHOLD = 0.005;

  /**
   * 대시보드 요약 데이터 조회
   * - 진행중인 배틀 수 (날짜 기반 판별)
   * - 전체 아이템 수
   * - 인기 피드 수 (hotScore 임계값 기준)
   */
  public DashboardSummaryResponse getDashboardSummary() {
    LocalDateTime now = LocalDateTime.now();

    return DashboardSummaryResponse.builder()
        .activeBattleCount(battleRepository.countActiveBattles(now))
        .totalItemCount(productRepository.countByStatusAndIsDeletedFalse(ProductStatus.APPROVED))
        .popularFeedCount(feedRepository.countByHotScoreGreaterThanEqualAndIsDeletedFalse(HOT_SCORE_THRESHOLD))
        .build();
  }
}
