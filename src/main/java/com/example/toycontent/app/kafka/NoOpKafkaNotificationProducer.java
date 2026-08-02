package com.example.toycontent.app.kafka;

import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
public class NoOpKafkaNotificationProducer extends KafkaNotificationProducer {

  public NoOpKafkaNotificationProducer() {
    super(null);
  }

  @Override
  public void send(KafkaNotificationDto dto) {
    log.info("[notify] 알림 스킵: userId={}, type={}", dto.getUserId(), dto.getType());
  }
}
