package com.example.toycontent.app.notification.outbox;

import com.example.toycontent.app.kafka.KafkaNotificationProducer;
import io.micrometer.observation.annotation.Observed;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * outbox 청소 릴레이. 커밋 직후 즉시 전송이 놓친 PENDING 행만 다시 보낸다.
 *
 * <p><b>왜 리더 한 대로 충분한가.</b> GUARANTEED 등급은 배틀 결과·D-7·승인 요청·초대·시스템이다.
 * 좋아요·댓글 같은 최다 발생 타입은 이 경로를 타지 않으므로 outbox에 쌓이는 양은
 * "배틀 종료 건수 x 참여자 수"와 "승인 요청 건수" 수준이고, 평상시 즉시 전송이 성공하면
 * 릴레이가 읽는 결과는 비어 있다. 릴레이가 실제로 일하는 것은 Kafka 장애·파드 종료 뒤 복구 구간뿐이다.
 *
 * <p><b>왜 첫 실패에서 회차를 끊는가.</b> 브로커에 못 붙으면 send가 max.block.ms(60초)까지 막힌다
 * (IN-2 실측). 200건을 순서대로 시도하면 락 최대 시간을 넘겨 다른 파드가 겹쳐 뛴다.
 * Kafka가 죽었으면 뒤의 행도 같이 실패할 것이므로 한 번 실패하면 이번 회차를 끝내고 다음 회차에 맡긴다.
 *
 * <p><b>왜 grace를 두는가.</b> 방금 커밋된 행은 즉시 전송이 진행 중일 수 있다. 즉시 전송의 실패 판정
 * (max.block.ms 60초)보다 긴 시간 동안은 릴레이가 집지 않아 둘이 같은 행을 보내는 중복을 줄인다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxRelay {

  private final NotificationOutboxRepository repository;
  private final NotificationOutboxStore store;
  private final KafkaNotificationProducer producer;

  @Value("${notification.outbox.relay.grace-seconds:90}")
  private long graceSeconds;

  @Value("${notification.outbox.relay.batch-size:200}")
  private int batchSize;

  @Value("${notification.outbox.relay.ack-timeout-seconds:30}")
  private long ackTimeoutSeconds;

  @Value("${notification.outbox.retention-days:7}")
  private long retentionDays;

  @Observed(name = "notification.outbox.relay", contextualName = "notification-outbox-relay")
  @Scheduled(fixedDelayString = "${notification.outbox.relay.delay-ms:30000}")
  @SchedulerLock(name = "notificationOutboxRelay", lockAtLeastFor = "5s", lockAtMostFor = "3m")
  public void relay() {
    LocalDateTime createdBefore = LocalDateTime.now().minusSeconds(graceSeconds);
    List<NotificationOutbox> batch = repository.findPendingBatch(createdBefore, PageRequest.of(0, batchSize));
    if (batch.isEmpty()) {
      return;
    }
    log.info("[outbox] 릴레이 시작: pending={}", batch.size());

    int sent = 0;
    for (NotificationOutbox row : batch) {
      try {
        producer.send(store.deserialize(row)).get(ackTimeoutSeconds, TimeUnit.SECONDS);
        store.settle(row.getId(), null);
        sent++;
      } catch (Exception e) {
        store.settle(row.getId(), e);
        log.warn("[outbox] 릴레이 전송 실패, 이번 회차 중단: id={}, type={}, userId={}, error={}",
            row.getId(), row.getType(), row.getUserId(), e.getMessage());
        break;
      }
    }
    log.info("[outbox] 릴레이 종료: sent={}/{}", sent, batch.size());
  }

  /** SENT 행 정리. 매일 04:30. */
  @Scheduled(cron = "0 30 4 * * *")
  @SchedulerLock(name = "notificationOutboxPurge", lockAtLeastFor = "1m", lockAtMostFor = "30m")
  @Transactional
  public void purgeSent() {
    LocalDateTime sentBefore = LocalDateTime.now().minus(Duration.ofDays(retentionDays));
    int deleted = repository.deleteSentBefore(sentBefore);
    long dead = repository.countByStatus(NotificationOutboxStatus.DEAD);
    log.info("[outbox] SENT 정리: deleted={}, retentionDays={}, dead={}", deleted, retentionDays, dead);
  }
}
