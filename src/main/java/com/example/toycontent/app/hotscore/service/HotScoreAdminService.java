package com.example.toycontent.app.hotscore.service;

import com.example.toycontent.app.battle.service.BattleHotScoreService;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.HotScoreErrorCode;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import com.example.toycontent.app.hotscore.controller.dto.HotScoreResponse.DivisorStatus;
import com.example.toycontent.app.hotscore.controller.dto.HotScoreResponse.RecalculateResult;
import com.example.toycontent.app.hotscore.domain.HotScoreDomain;
import com.example.toycontent.app.hotscore.domain.HotScoreSettings;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * 관리자용 핫 스코어 운영 기능. 컨트롤러는 HTTP 매핑만 하고 판단은 여기서 한다.
 *
 * <ul>
 *   <li>{@link #current()} — 도메인별 시간 상수 현황</li>
 *   <li>{@link #recalculate(HotScoreDomain)} — 저장된 점수를 지금 상수로 전부 다시 씀</li>
 *   <li>{@link #changeDivisor(HotScoreDomain, long)} — 상수 변경 후 곧바로 전체 재계산</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotScoreAdminService {

  private final HotScoreConfigService config;
  private final FeedHotScoreService feed;
  private final BattleHotScoreService battle;
  private final ProductPopularityService product;

  public List<DivisorStatus> current() {
    return config.current().entrySet().stream()
        .map(e -> new DivisorStatus(e.getKey().key(), e.getValue().timeDivisorSeconds(),
            e.getValue().defaultSeconds(), e.getValue().overridden()))
        .toList();
  }

  public RecalculateResult recalculate(HotScoreDomain domain) {
    StopWatch watch = new StopWatch();
    watch.start();
    int count = switch (domain) {
      case FEED -> feed.recalculateAll();
      case BATTLE -> battle.recalculateAll();
      case PRODUCT -> product.recalculateAll();
    };
    watch.stop();
    return new RecalculateResult(domain.key(), HotScoreSettings.of(domain), count, watch.getTotalTimeMillis());
  }

  public RecalculateResult changeDivisor(HotScoreDomain domain, Long seconds) {
    if (seconds == null) {
      throw new RestApiException(HotScoreErrorCode.INVALID_TIME_DIVISOR);
    }
    try {
      config.update(domain, seconds);
    } catch (IllegalArgumentException e) {
      throw new RestApiException(HotScoreErrorCode.INVALID_TIME_DIVISOR);
    }
    RecalculateResult result = recalculate(domain);
    log.info("[hot-score] 상수 변경 + 전체 재계산 - {} = {}s, {}건, {}ms",
        domain.key(), seconds, result.recalculated(), result.elapsedMs());
    return result;
  }

}
