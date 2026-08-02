package com.example.toycontent.app.notification;

import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
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
 *   <li><b>fallbackExecution = true</b>: 트랜잭션 밖에서 발행된 이벤트도 그대로 처리한다
 *       (트랜잭션 경계가 없는 호출 경로에 대한 안전장치).</li>
 *   <li><b>{@code @Async}</b>: 커밋 스레드를 막지 않고 별도 풀에서 발행해 부하를 격리한다.</li>
 * </ul>
 *
 * <p>주의: AFTER_COMMIT 단계의 예외는 이미 커밋된 트랜잭션을 되돌리지 못한다.
 * 본 도메인은 알림 유실이 허용되므로 이 구간 실패는 로깅으로 보완한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final KafkaNotificationProducer notificationProducer;
  private final Tracer tracer;

  @Observed(name = "notification.publish",
      contextualName = "notification-publish")
  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onNotification(NotificationEvent event) {
    KafkaNotificationDto dto = event.payload();
    try {
      notificationProducer.send(dto);
    } catch (Exception e) {
      // 예외를 삼키더라도 현재 span에는 error를 명시 기록 —
      // tail sampling "에러 100% 보존" 정책이 이 실패를 놓치지 않도록.
      Span current = tracer.currentSpan();
      if (current != null) {
        current.error(e);
      }
      log.error("[notify] 알림 발행 실패: userId={}, type={}, error={}",
          dto.getUserId(), dto.getType(), e.getMessage(), e);
    }
  }
}
