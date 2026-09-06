package com.example.toycontent.app.notification.outbox;

import com.example.toycontent.app.common.enumuration.NotificationType;
import com.example.toycontent.app.kafka.dto.KafkaNotificationDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * outbox 행의 생성과 상태 전이. 직렬화 한 곳, 상태 전이 한 곳.
 *
 * <p>{@link #enqueue}는 호출자의 트랜잭션에 참여한다 (도메인 저장과 같은 커밋).
 * {@link #settle}은 커밋 뒤 비동기 스레드나 릴레이에서 불리므로 짧은 트랜잭션을 새로 연다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxStore {

  private final NotificationOutboxRepository repository;
  private final ObjectMapper objectMapper;

  @Value("${notification.outbox.max-attempts:20}")
  private int maxAttempts;

  /** 호출자 트랜잭션 안에서 PENDING 행을 남긴다. 트랜잭션이 없으면 그 자체로 커밋된다. */
  @Transactional(propagation = Propagation.REQUIRED)
  public Long enqueue(NotificationType type, KafkaNotificationDto dto) {
    NotificationOutbox row = NotificationOutbox.pending(type, dto.getUserId(), serialize(dto));
    return repository.save(row).getId();
  }

  /**
   * 전송 결과를 행에 반영한다. {@code failure}가 null이면 성공(SENT), 아니면 실패(시도 횟수 증가,
   * 최대 시도에 닿으면 DEAD). 이미 PENDING이 아닌 행은 건드리지 않는다 (즉시 전송과 릴레이가 겹친 경우).
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void settle(Long outboxId, Throwable failure) {
    repository.findById(outboxId)
        .filter(NotificationOutbox::isPending)
        .ifPresent(row -> {
          row.settle(failure, maxAttempts);
          log.info("[outbox] 상태 반영: id={}, type={}, userId={}, status={}, attempts={}, lastError={}",
              row.getId(), row.getType(), row.getUserId(), row.getStatus(), row.getAttemptCount(), row.getLastError());
        });
  }

  public KafkaNotificationDto deserialize(NotificationOutbox row) {
    try {
      return objectMapper.readValue(row.getPayload(), KafkaNotificationDto.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("outbox payload 역직렬화 실패: id=" + row.getId(), e);
    }
  }

  private String serialize(KafkaNotificationDto dto) {
    try {
      return objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("outbox payload 직렬화 실패: type=" + dto.getType(), e);
    }
  }
}
