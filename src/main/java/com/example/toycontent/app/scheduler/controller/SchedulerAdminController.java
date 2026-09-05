package com.example.toycontent.app.scheduler.controller;

import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.SchedulerErrorCode;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.battle.service.BattleHotScoreService;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import com.example.toycontent.app.scheduler.BattleDeadlineNotificationScheduler;
import com.example.toycontent.app.scheduler.FeedTrendingScheduler;
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
 * 배치 작업 수동 실행 (ADMIN 전용).
 *
 * <p>핫 스코어(피드·배틀·제품)는 참여가 생기는 행에서 바로 갱신되므로 정기 배치가 없다.
 * 여기의 {@code *.recalculate}는 시간 상수({@code hot-score.*-time-divisor-seconds})를 바꾼 뒤
 * 저장된 점수를 새 기준으로 맞출 때 한 번 돌리는 도구다.</p>
 *
 * <p>트렌딩 스냅샷·배틀 마감 알림은 cron 스케줄러 빈을 그대로 호출하므로 {@code @SchedulerLock}이
 * 동일하게 적용된다. 락이 잡혀 있으면 본문이 건너뛰어지고 즉시 반환된다.</p>
 */
@Tag(name = "SchedulerAdminController", description = "배치 작업 수동 실행 (ADMIN 전용)")
@RestController
@Slf4j
@RequestMapping("/admin/schedulers")
public class SchedulerAdminController {

  private final Map<String, Runnable> jobs;

  public SchedulerAdminController(FeedHotScoreService feedHotScore,
                                  BattleHotScoreService battleHotScore,
                                  ProductPopularityService productPopularity,
                                  FeedTrendingScheduler feedTrending,
                                  BattleDeadlineNotificationScheduler battleDeadline) {
    Map<String, Runnable> map = new LinkedHashMap<>();
    map.put("feed-hot-score.recalculate", feedHotScore::recalculateAll);          // 상수 변경 후 1회
    map.put("battle-hot-score.recalculate", battleHotScore::recalculateAll);      // 상수 변경 후 1회
    map.put("product-popularity.recalculate", productPopularity::recalculateAll); // 상수 변경 후 1회
    map.put("feed-trending", feedTrending::updateTrendingAndSnapshot);            // 매일 00:15
    map.put("battle-deadline.d7", battleDeadline::notifyD7);                      // 매 정각
    map.put("battle-deadline.end", battleDeadline::notifyEnd);                    // 매 분
    this.jobs = Map.copyOf(map);
  }

  @Operation(summary = "수동 실행 가능한 스케줄러 작업 목록")
  @GetMapping
  public ResponseEntity<ApiResponse<List<String>>> list(@CurrentUserIsAdmin boolean isAdmin) {
    requireAdmin(isAdmin);
    return ResponseEntity.ok(ApiResponse.success(List.copyOf(jobs.keySet())));
  }

  @Operation(summary = "스케줄러 작업 즉시 실행",
      description = "동기로 실행하고 소요 시간을 돌려준다. *.recalculate는 시간 상수 변경 후에만 쓴다. cron 작업은 ShedLock이 잡혀 있으면 본문이 건너뛰어져 elapsedMs가 수 ms로 나온다.")
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

  /**
   * @param elapsedMs 컨트롤러가 잰 벽시계 시간. 스케줄러 자체 로그의 ms와 거의 같다.
   *                  ShedLock에 걸려 건너뛰었으면 수 ms로 나온다.
   */
  public record RunResult(String job, LocalDateTime startedAt, long elapsedMs) {
  }
}
