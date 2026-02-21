package com.example.toycontent.app.scheduler;


import com.example.toycontent.app.product.service.ProductPopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPopularityScheduler {
  private final ProductPopularityService popularityService;



  @Value("${scheduler.product-popularity.time-weight-update.enabled}")
  private Boolean timeWeightEnabled;

  @Value("${scheduler.product-popularity.full-recalculate.enabled}")
  private Boolean fullRecalculateEnabled;
  /**
   * 시간 가중치 업데이트 (매시간)
   * - 최근 활동이 있거나 dirty 마킹된 상품만 처리
   */
  @Scheduled(cron = "${scheduler.product-popularity.time-weight-update.cron}")
  @SchedulerLock(
      name = "productPopularity",
      lockAtLeastFor = "30m",
      lockAtMostFor = "50m"
  )
  public void timeWeightUpdate() {
    if (!timeWeightEnabled) {
      log.debug("[제품 인기도] 시간 가중치 업데이트 스케줄러 OFF");
      return;
    }

    log.info("[Product Popularity] 시간 가중치 업데이트 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      int count = popularityService.updateDirtyProducts();
      stopWatch.stop();
      log.info("[제품 인기도] 시간 가중치 업데이트 완료 - {}건, {}ms",
          count, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[제품 인기도] 시간 가중치 업데이트 실패", e);
    }
  }

  /**
   * 전체 재계산 (새벽 3시)
   * - 시간 감쇠 반영을 위해 전체 상품 재계산
   */
  @SchedulerLock(
      name = "productPopularity",
      lockAtLeastFor = "1h",
      lockAtMostFor = "6h"
  )
  @Scheduled(cron = "${scheduler.product-popularity.full-recalculate.cron}")
  public void fullRecalculate() {
    if (!fullRecalculateEnabled) {
      log.debug("[제품 인기도] 전체 재계산 스케줄러 OFF");
      return;
    }

    log.info("[제품 인기도] 전체 재계산 시작");
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      int count = popularityService.recalculateAll();
      stopWatch.stop();
      log.info("[제품 인기도] 전체 재계산 완료 - {}건, {}ms",
          count, stopWatch.getTotalTimeMillis());
    } catch (Exception e) {
      log.error("[제품 인기도] 전체 재계산 실패", e);
    }
  }
}
