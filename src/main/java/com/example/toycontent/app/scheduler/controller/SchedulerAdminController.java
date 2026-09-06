package com.example.toycontent.app.scheduler.controller;

import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.SchedulerErrorCode;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.battle.service.BattleHotScoreService;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 핫 스코어 전체 재계산 (ADMIN 전용).
 *
 * <p>핫 스코어(피드·배틀·제품)는 참여가 생기는 행에서 바로 갱신되므로 정기 배치가 없다.
 * 이 API는 시간 상수({@code hot-score.*-time-divisor-seconds})를 바꾼 뒤 저장된 점수를
 * 새 기준으로 맞출 때 한 번 돌리는 도구다. 배포 직후 옛 공식 점수를 새 척도로 바꿀 때도 쓴다.</p>
 */
@Tag(name = "SchedulerAdminController", description = "핫 스코어 전체 재계산 (ADMIN 전용)")
@RestController
@Slf4j
@RequestMapping("/admin/schedulers")
public class SchedulerAdminController {

  private final Map<String, Runnable> jobs;

  public SchedulerAdminController(FeedHotScoreService feedHotScore,
                                  BattleHotScoreService battleHotScore,
                                  ProductPopularityService productPopularity) {
    Map<String, Runnable> map = new LinkedHashMap<>();
    map.put("feed-hot-score.recalculate", feedHotScore::recalculateAll);          // 피드 · 상수 14일
    map.put("battle-hot-score.recalculate", battleHotScore::recalculateAll);      // 배틀 · 상수 30일
    map.put("product-popularity.recalculate", productPopularity::recalculateAll); // 제품 · 상수 30일
    this.jobs = java.util.Collections.unmodifiableMap(map); // 등록 순서 유지
  }

  @Operation(summary = "전체 재계산 작업 목록")
  @GetMapping
  public ResponseEntity<ApiResponse<List<String>>> list(@CurrentUserIsAdmin boolean isAdmin) {
    requireAdmin(isAdmin);
    return ResponseEntity.ok(ApiResponse.success(List.copyOf(jobs.keySet())));
  }

  @Operation(summary = "전체 재계산 즉시 실행",
      description = "동기로 실행하고 소요 시간을 돌려준다. 시간 상수 변경 후 또는 배포 직후에만 쓴다.")
  @PostMapping("/{job}")
  public ResponseEntity<ApiResponse<RunResult>> run(@PathVariable String job,
                                                    @CurrentUserIsAdmin boolean isAdmin) {
    requireAdmin(isAdmin);
    Runnable target = jobs.get(job);
    if (target == null) {
      throw new RestApiException(SchedulerErrorCode.JOB_NOT_FOUND);
    }

    LocalDateTime startedAt = LocalDateTime.now();
    log.info("[scheduler-admin] 수동 실행 시작 - job={}", job);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    target.run();
    stopWatch.stop();
    long elapsedMs = stopWatch.getTotalTimeMillis();
    log.info("[scheduler-admin] 수동 실행 완료 - job={}, {}ms", job, elapsedMs);

    return ResponseEntity.ok(ApiResponse.success(new RunResult(job, startedAt, elapsedMs)));
  }

  private static void requireAdmin(boolean isAdmin) {
    if (!isAdmin) {
      throw new RestApiException(SchedulerErrorCode.ADMIN_ONLY);
    }
  }

  /** @param elapsedMs 컨트롤러가 잰 벽시계 시간. 서비스 로그의 ms와 거의 같다. */
  public record RunResult(String job, LocalDateTime startedAt, long elapsedMs) {
  }
}
