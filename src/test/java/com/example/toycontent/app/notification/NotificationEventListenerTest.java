package com.example.toycontent.app.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener - 커밋 이후 실제 발행")
class NotificationEventListenerTest {

  @Mock private KafkaNotificationProducer notificationProducer;
  @Mock private Tracer tracer;

  @InjectMocks private NotificationEventListener listener;

  private NotificationEvent event() {
    return new NotificationEvent(KafkaNotificationDto.builder()
        .userId(100L)
        .type(NotificationType.FEED_LIKE)
        .build());
  }

  @Test
  @DisplayName("이벤트를 수신하면 Kafka producer로 DTO를 발행한다")
  void 이벤트_수신시_발행() {
    // when
    listener.onNotification(event());

    // then
    then(notificationProducer).should().send(any(KafkaNotificationDto.class));
  }

  @Test
  @DisplayName("Kafka producer가 예외를 던져도 리스너는 정상 반환된다 (유실 허용 도메인)")
  void producer_예외_삼킴() {
    // given
    willThrow(new RuntimeException("Kafka down"))
        .given(notificationProducer).send(any());

    // when & then - 예외가 밖으로 전파되지 않아야 함
    listener.onNotification(event());

    then(notificationProducer).should().send(any());
  }
}
