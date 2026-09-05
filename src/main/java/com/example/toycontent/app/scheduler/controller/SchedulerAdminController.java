package com.example.toycontent.app.scheduler.controller;

import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.SchedulerErrorCode;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.scheduler.BattleDeadlineNotificationScheduler;
import com.example.toycontent.app.scheduler.BattleHotScoreScheduler;
import com.example.toycontent.app.scheduler.FeedHotScoreScheduler;
import com.example.toycontent.app.scheduler.FeedTrendingScheduler;
import com.example.toycontent.app.scheduler.ProductPopularityScheduler;
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
 * 스케줄러 수동 실행 (ADMIN 전용).
 *
 * <p>cron을 기다리지 않고 같은 작업을 지금 돌린다. 대량 적재 직후 핫 스코어를 바로 맞추거나,
 * 데이터 양이 바뀐 뒤 각 작업이 얼마나 걸리는지 실측할 때 쓴다.</p>
 *
 * <p>스케줄러 빈의 메서드를 그대로 호출하므로 {@code @SchedulerLock} · {@code @Transactional} ·
 * {@code @CacheEvict}가 cron 실행과 동일하게 적용된다. 따라서 <b>ShedLock이 잡혀 있으면
 * (예: 정각 실행 직후 lockAtLeastFor 안) 본문이 실행되지 않고 즉시 반환된다.</b>
 * 실제로 돌았는지는 응답의 {@code elapsedMs}와 {@code [scheduler] ... 완료} 로그로 확인한다.</p>
 */
@Tag(name = "SchedulerAdminController", description = "스케줄러 수동 실행 (ADMIN 전용)")
@RestController
@Slf4j
@RequestMapping("/admin/schedulers")
public class SchedulerAdminController {

  private final Map<String, Runnable> jobs;

  public SchedulerAdminController(FeedHotScoreScheduler feedHotScore,
                                  BattleHotScoreScheduler battleHotScore,
                                  ProductPopularityScheduler productPopularity,
                                  FeedTrendingScheduler feedTrending,
                                  BattleDeadlineNotificationScheduler battleDeadline) {
    Map<String, Runnable> map = new LinkedHashMap<>();
    map.put("feed-hot-score.time-weight", feedHotScore::timeWeightUpdate);      // 매 정각
    map.put("feed-hot-score.full", feedHotScore::fullRecalculate);              // 매일 03:00
    map.put("battle-hot-score.time-weight", battleHotScore::timeWeightUpdate);  // 매 정각
    map.put("battle-hot-score.full", battleHotScore::fullRecalculate);          // 매일 02:00
    map.put("product-popularity.time-weight", productPopularity::timeWeightUpdate); // 매시 30분
    map.put("product-popularity.full", productPopularity::fullRecalculate);     // 매일 01:00
    map.put("feed-trending", feedTrending::updateTrendingAndSnapshot);          // 매일 00:15
    map.put("battle-deadline.d7", battleDeadline::notifyD7);                    // 매 정각
    map.put("battle-deadline.end", battleDeadline::notifyEnd);                  // 매 분
    this.jobs = Map.copyOf(map);
  }

  @Operation(summary = "수동 실행 가능한 스케줄러 작업 목록")
  @GetMapping
  public ResponseEntity<ApiResponse<List<String>>> list(@CurrentUserIsAdmin boolean isAdmin) {
    requireAdmin(isAdmin);
    return ResponseEntity.ok(ApiResponse.success(List.copyOf(jobs.keySet())));
  }

  @Operation(summary = "스케줄러 작업 즉시 실행",
      description = "동기로 실행하고 소요 시간을 돌려준다. ShedLock이 잡혀 있으면 본문이 건너뛰어져 elapsedMs가 수 ms로 나온다.")
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
