package com.example.toycontent.app.kafka;

import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaNotificationProducer {

  @Value("${spring.kafka.topic.notification}")
  private String topic;

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * DTO 직접 발행. 결과 Future를 돌려주므로 호출자가 ack를 기다리거나(릴레이) 콜백을 달 수 있다(리스너).
   * 성공·실패 로그는 여기서 한 번만 남긴다.
   */
  public CompletableFuture<SendResult<String, Object>> send(KafkaNotificationDto dto) {
    String key = String.valueOf(dto.getUserId());
    return kafkaTemplate.send(topic, key, dto)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("[notify] 알림 발행 실패: userId={}, type={}, error={}",
                dto.getUserId(), dto.getType(), ex.getMessage(), ex);
          } else {
            log.info("[notify] 알림 발행 성공: userId={}, type={}, partition={}, offset={}",
                dto.getUserId(), dto.getType(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
