package com.example.toycontent.app.kafka;

import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
public class NoOpKafkaNotificationProducer extends KafkaNotificationProducer {

  public NoOpKafkaNotificationProducer() {
    super(null);
  }

  /** Kafka가 꺼진 환경. 성공한 것으로 간주해 outbox 행이 PENDING으로 쌓이지 않게 한다. */
  @Override
  public CompletableFuture<SendResult<String, Object>> send(KafkaNotificationDto dto) {
    log.info("[notify] 알림 스킵: userId={}, type={}", dto.getUserId(), dto.getType());
    return CompletableFuture.completedFuture(null);
  }
}
