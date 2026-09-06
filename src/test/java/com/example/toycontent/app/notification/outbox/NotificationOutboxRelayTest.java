package com.example.toycontent.app.notification.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationOutboxRelay - PENDING 청소")
class NotificationOutboxRelayTest {

  @Mock private NotificationOutboxRepository repository;
  @Mock private NotificationOutboxStore store;
  @Mock private KafkaNotificationProducer producer;

  @InjectMocks private NotificationOutboxRelay relay;

  @BeforeEach
  void config() {
    ReflectionTestUtils.setField(relay, "graceSeconds", 90L);
    ReflectionTestUtils.setField(relay, "batchSize", 200);
    ReflectionTestUtils.setField(relay, "ackTimeoutSeconds", 5L);
    ReflectionTestUtils.setField(relay, "retentionDays", 7L);
  }

  private static NotificationOutbox row(long id) {
    return NotificationOutbox.builder()
        .id(id).type(NotificationType.BATTLE_RESULT).userId(id * 10)
        .payload("{}").status(NotificationOutboxStatus.PENDING).build();
  }

  private static KafkaNotificationDto dto() {
    return KafkaNotificationDto.builder().userId(1L).type(NotificationType.BATTLE_RESULT).build();
  }

  private static CompletableFuture<SendResult<String, Object>> acked() {
    return CompletableFuture.completedFuture(null);
  }

  @Test
  @DisplayName("평상시 - PENDING이 없으면 아무것도 보내지 않는다 (즉시 전송이 다 처리한 상태)")
  void 빈_배치() {
    given(repository.findPendingBatch(any(), any())).willReturn(List.of());

    relay.relay();

    then(producer).should(never()).send(any());
  }

  @Test
  @DisplayName("PENDING 행을 created_at 순으로 보내고 ack마다 성공을 반영한다")
  void 정상_청소() {
    given(repository.findPendingBatch(any(), any())).willReturn(List.of(row(1), row(2), row(3)));
    given(store.deserialize(any())).willReturn(dto());
    given(producer.send(any())).willReturn(acked());

    relay.relay();

    then(producer).should(times(3)).send(any());
    then(store).should().settle(eq(1L), isNull());
    then(store).should().settle(eq(2L), isNull());
    then(store).should().settle(eq(3L), isNull());
  }

  @Test
  @DisplayName("첫 실패에서 이번 회차를 끊는다 - Kafka가 죽었으면 뒤도 실패하고 락 시간만 소진하기 때문")
  void 첫_실패에서_중단() {
    given(repository.findPendingBatch(any(), any())).willReturn(List.of(row(1), row(2), row(3)));
    given(store.deserialize(any())).willReturn(dto());
    given(producer.send(any()))
        .willReturn(acked())
        .willReturn(CompletableFuture.failedFuture(new RuntimeException("broker unreachable")));

    relay.relay();

    then(producer).should(times(2)).send(any());
    then(store).should().settle(eq(1L), isNull());
    then(store).should().settle(eq(2L), any(Throwable.class));
    then(store).should(never()).settle(eq(3L), any());
  }

  @Test
  @DisplayName("역직렬화가 깨진 행은 실패로 반영하고 회차를 끊는다 (반복되면 DEAD로 내려가 운영자가 본다)")
  void 역직렬화_실패() {
    IllegalStateException bad = new IllegalStateException("bad payload");
    given(repository.findPendingBatch(any(), any())).willReturn(List.of(row(1)));
    given(store.deserialize(any())).willThrow(bad);

    relay.relay();

    then(producer).should(never()).send(any());
    then(store).should().settle(1L, bad);
  }

  @Test
  @DisplayName("엔티티 settle - 성공은 SENT, 실패는 시도 누적, 최대 시도에 닿으면 DEAD")
  void 엔티티_상태_전이() {
    NotificationOutbox ok = row(1);
    ok.settle(null, 20);
    assertThat(ok.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
    assertThat(ok.getSentAt()).isNotNull();

    NotificationOutbox failing = row(2);
    for (int i = 0; i < 19; i++) {
      failing.settle(new RuntimeException("e"), 20);
    }
    assertThat(failing.isPending()).isTrue();
    assertThat(failing.getLastError()).contains("e");

    failing.settle(new RuntimeException("e"), 20);
    assertThat(failing.getStatus()).isEqualTo(NotificationOutboxStatus.DEAD);
    assertThat(failing.getAttemptCount()).isEqualTo(20);
  }
}
