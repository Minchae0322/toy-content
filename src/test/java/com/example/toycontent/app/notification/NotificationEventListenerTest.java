package com.example.toycontent.app.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import com.example.toycontent.app.notification.outbox.NotificationOutboxStore;
import io.micrometer.tracing.Tracer;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener - 커밋 이후 실제 발행")
class NotificationEventListenerTest {

  @Mock private KafkaNotificationProducer notificationProducer;
  @Mock private NotificationOutboxStore outboxStore;
  @Mock private Tracer tracer;

  @InjectMocks private NotificationEventListener listener;

  private static KafkaNotificationDto dto(NotificationType type) {
    return KafkaNotificationDto.builder().userId(100L).type(type).build();
  }

  private static CompletableFuture<SendResult<String, Object>> acked() {
    return CompletableFuture.completedFuture(null);
  }

  private static CompletableFuture<SendResult<String, Object>> failed(Throwable t) {
    return CompletableFuture.failedFuture(t);
  }

  @Test
  @DisplayName("BEST_EFFORT 이벤트는 producer로 발행되고 outbox는 건드리지 않는다")
  void best_effort_발행() {
    given(notificationProducer.send(any())).willReturn(acked());

    listener.onNotification(new NotificationEvent(dto(NotificationType.FEED_LIKE)));

    then(notificationProducer).should().send(any(KafkaNotificationDto.class));
    then(outboxStore).should(never()).settle(anyLong(), any());
  }

  @Test
  @DisplayName("BEST_EFFORT 발행이 실패해도 리스너는 정상 반환되고 outbox는 건드리지 않는다 (유실 허용)")
  void best_effort_실패_삼킴() {
    given(notificationProducer.send(any())).willReturn(failed(new RuntimeException("Kafka down")));

    listener.onNotification(new NotificationEvent(dto(NotificationType.FEED_LIKE)));

    then(outboxStore).should(never()).settle(anyLong(), any());
  }

  @Test
  @DisplayName("GUARANTEED 이벤트는 ack를 받으면 outbox 행에 성공을 반영한다")
  void guaranteed_ack시_성공_반영() {
    given(notificationProducer.send(any())).willReturn(acked());

    listener.onNotification(new NotificationEvent(dto(NotificationType.BATTLE_RESULT), 77L));

    then(outboxStore).should().settle(eq(77L), isNull());
  }

  @Test
  @DisplayName("GUARANTEED 발행이 실패하면 실패를 반영하고 행은 PENDING으로 남겨 릴레이에 맡긴다")
  void guaranteed_실패시_실패_반영() {
    RuntimeException down = new RuntimeException("Kafka down");
    given(notificationProducer.send(any())).willReturn(failed(down));

    listener.onNotification(new NotificationEvent(dto(NotificationType.BATTLE_RESULT), 77L));

    then(outboxStore).should().settle(77L, down);
  }

  @Test
  @DisplayName("send가 동기로 예외를 던져도 리스너는 정상 반환되고 GUARANTEED면 실패로 반영한다")
  void send_동기_예외() {
    IllegalStateException notReady = new IllegalStateException("producer not ready");
    willThrow(notReady).given(notificationProducer).send(any());

    listener.onNotification(new NotificationEvent(dto(NotificationType.SYSTEM), 5L));

    then(outboxStore).should().settle(5L, notReady);
  }
}
