package com.example.toycontent.app.feed.event;

import com.example.toycontent.app.common.hotscore.HotScoreSettings;
import com.example.toycontent.app.feed.repository.FeedRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 조회수 증가를 요청 경로 밖에서 처리한다 (notification의 NotificationEventListener와 같은 패턴).
 *
 * <ul>
 *   <li><b>AFTER_COMMIT + fallbackExecution</b>: 조회 트랜잭션이 끝난 뒤에만 실행되고,
 *       트랜잭션 없는 호출 경로에서도 동작한다.</li>
 *   <li><b>{@code @Async("viewCountExecutor")}</b>: 응답 스레드를 막지 않는다. 포화 시 큐가
 *       차면 CallerRuns로 되밀리므로 증가분이 유실되지는 않는다 (대가는 그 요청의 지연).</li>
 *   <li><b>REQUIRES_NEW</b>: AFTER_COMMIT 단계에서 원래(이미 완료된) 트랜잭션에 참여하면
 *       쓰기가 flush되지 않는 고전 함정 - 반드시 새 트랜잭션에서 UPDATE한다.</li>
 *   <li><b>원자 UPDATE 유지</b>: 엔티티 로드+저장으로 바꾸면 동시 증가가 서로를 덮어쓴다.
 *       {@code incrementViewCount}(단문 UPDATE)를 그대로 쓴다.</li>
 * </ul>
 *
 * <p>주의: 이 구간의 실패·파드 종료 시 큐 잔량은 조회수 유실이다 (수용된 트레이드오프 -
 * 앱개선.md C-3). 실패는 로그 + 현재 span error로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedViewCountEventListener {

  private final FeedRepository feedRepository;
  private final Tracer tracer;

  @Async("viewCountExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onFeedViewed(FeedViewedEvent event) {
    try {
      feedRepository.incrementViewCount(event.feedId(), HotScoreSettings.feedDivisor());
    } catch (Exception e) {
      Span current = tracer.currentSpan();
      if (current != null) {
        current.error(e);
      }
      log.error("[view-count] 조회수 증가 실패: feedId={}, error={}", event.feedId(), e.getMessage(), e);
    }
  }
}
