package com.example.toycontent.app.scheduler.controller;

import com.example.toycontent.app.battle.service.BattleHotScoreService;
import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.SchedulerErrorCode;
import com.example.toycontent.app.common.hotscore.HotScoreConfigService;
import com.example.toycontent.app.common.hotscore.HotScoreDomain;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 핫 스코어 시간 상수 조회·변경 (ADMIN 전용).
 *
 * <p>공식이 {@code log10(참여도) + 기준시각/상수} 라서 상수를 바꾸면 저장된 점수 전체의 기준이
 * 달라진다. 그래서 {@link #update}는 <b>상수 저장 → 즉시 그 도메인 전체 재계산</b>을 한 트랜잭션
 * 흐름으로 묶는다. 값은 Redis에 남아 재시작 후에도 유지되고, 다른 파드는 30초 안에 따라온다.</p>
 *
 * <p>단위는 초. "참여도 10배 = 이 시간" 이 뜻이다. 예) 1,209,600 = 14일, 2,592,000 = 30일.</p>
 */
@Tag(name = "HotScoreAdminController", description = "핫 스코어 시간 상수 조회·변경 (ADMIN 전용)")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/hot-score")
public class HotScoreAdminController {

  private final HotScoreConfigService configService;
  private final FeedHotScoreService feedHotScore;
  private final BattleHotScoreService battleHotScore;
  private final ProductPopularityService productPopularity;

  @Operation(summary = "도메인별 시간 상수 현재값")
  @GetMapping
  public ResponseEntity<ApiResponse<List<ConfigView>>> current(@CurrentUserIsAdmin boolean isAdmin) {
    requireAdmin(isAdmin);
    List<ConfigView> views = configService.current().entrySet().stream()
        .map(e -> ConfigView.of(e.getKey(), e.getValue()))
        .toList();
    return ResponseEntity.ok(ApiResponse.success(views));
  }

  @Operation(summary = "시간 상수 변경 후 그 도메인 전체 재계산",
      description = "body.timeDivisorSeconds 를 저장하고 곧바로 전체 재계산을 돌린다. 동기 실행이라 재계산이 끝나야 응답한다.")
  @PutMapping("/{domain}")
  public ResponseEntity<ApiResponse<UpdateResult>> update(@PathVariable String domain,
                                                          @RequestBody UpdateRequest request,
                                                          @CurrentUserIsAdmin boolean isAdmin) {
    requireAdmin(isAdmin);
    HotScoreDomain target = parse(domain);
    if (request == null || request.timeDivisorSeconds() == null) {
      throw new RestApiException(SchedulerErrorCode.INVALID_TIME_DIVISOR);
    }

    long applied;
    try {
      applied = configService.update(target, request.timeDivisorSeconds());
    } catch (IllegalArgumentException e) {
      throw new RestApiException(SchedulerErrorCode.INVALID_TIME_DIVISOR);
    }

    LocalDateTime startedAt = LocalDateTime.now();
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    int recalculated = switch (target) {
      case FEED -> feedHotScore.recalculateAll();
      case BATTLE -> battleHotScore.recalculateAll();
      case PRODUCT -> productPopularity.recalculateAll();
    };
    stopWatch.stop();
    log.info("[hot-score] 상수 변경 + 전체 재계산 - {} = {}s, {}건, {}ms",
        target.key(), applied, recalculated, stopWatch.getTotalTimeMillis());

    return ResponseEntity.ok(ApiResponse.success(
        new UpdateResult(target.key(), applied, applied / 86400.0, recalculated, startedAt,
            stopWatch.getTotalTimeMillis())));
  }

  private static HotScoreDomain parse(String domain) {
    try {
      return HotScoreDomain.valueOf(domain.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new RestApiException(SchedulerErrorCode.DOMAIN_NOT_FOUND);
    }
  }

  private static void requireAdmin(boolean isAdmin) {
    if (!isAdmin) {
      throw new RestApiException(SchedulerErrorCode.ADMIN_ONLY);
    }
  }

  public record UpdateRequest(Long timeDivisorSeconds) {
  }

  /**
   * @param timeDivisorSeconds 지금 적용 중인 값(초)
   * @param days 같은 값을 일 단위로
   * @param defaultSeconds yml 기본값
   * @param overridden Redis에 저장된 값이 기본값을 덮어쓰고 있는지
   */
  public record ConfigView(String domain, long timeDivisorSeconds, double days, long defaultSeconds,
                           boolean overridden) {
    static ConfigView of(HotScoreDomain d, HotScoreConfigService.Entry e) {
      return new ConfigView(d.key(), e.timeDivisorSeconds(), e.timeDivisorSeconds() / 86400.0,
          e.defaultSeconds(), e.overridden());
    }
  }

  public record UpdateResult(String domain, long timeDivisorSeconds, double days, int recalculated,
                             LocalDateTime startedAt, long elapsedMs) {
  }
}
