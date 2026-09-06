package com.example.toycontent.app.notification;

import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import com.example.toycontent.app.notification.outbox.NotificationOutboxStore;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 발행은 오직 트랜잭션 커밋 이후에만 일어난다.
 *
 * <ul>
 *   <li><b>AFTER_COMMIT</b>: 콘텐츠 저장이 롤백되면 이벤트도 함께 폐기되므로,
 *       존재하지 않는 데이터에 대한 "유령 알림"이 구조적으로 차단된다.</li>
 *   <li><b>fallbackExecution = true</b>: 트랜잭션 밖에서 발행된 이벤트도 그대로 처리한다.</li>
 *   <li><b>{@code @Async}</b>: 커밋 스레드를 막지 않고 별도 풀에서 발행해 부하를 격리한다.</li>
 * </ul>
 *
 * <p>등급별 차이는 ack 뒤에만 있다. GUARANTEED 이벤트는 같은 트랜잭션에 남긴 outbox 행이 있으므로
 * 전송 결과를 그 행에 반영한다(ack면 SENT, 실패면 PENDING 유지 후 릴레이가 재전송).
 * BEST_EFFORT는 결과를 로그로만 남긴다 (유실 허용).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final KafkaNotificationProducer notificationProducer;
  private final NotificationOutboxStore outboxStore;
  private final Tracer tracer;

  @Observed(name = "notification.publish",
      contextualName = "notification-publish")
  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onNotification(NotificationEvent event) {
    CompletableFuture<?> sent = send(event.payload());
    if (event.isGuaranteed()) {
      sent.whenComplete((result, failure) -> outboxStore.settle(event.outboxId(), failure));
    }
  }

  /**
   * send가 동기로 던진 예외(직렬화 실패, 프로듀서 미초기화 등)도 실패한 Future로 바꿔
   * 호출자가 한 가지 방식으로만 결과를 다루게 한다. AFTER_COMMIT 단계의 예외는 이미 커밋된
   * 트랜잭션을 되돌리지 못하므로 밖으로 던지지 않고, 현재 span에 error만 남긴다
   * (tail sampling "에러 100% 보존" 정책이 이 실패를 놓치지 않도록).
   */
  private CompletableFuture<?> send(KafkaNotificationDto dto) {
    try {
      return notificationProducer.send(dto);
    } catch (Exception e) {
      Span current = tracer.currentSpan();
      if (current != null) {
        current.error(e);
      }
      log.error("[notify] 알림 발행 예외: userId={}, type={}, error={}",
          dto.getUserId(), dto.getType(), e.getMessage(), e);
      return CompletableFuture.failedFuture(e);
    }
  }
}
