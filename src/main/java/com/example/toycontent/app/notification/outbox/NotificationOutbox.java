package com.example.toycontent.app.notification.outbox;

import com.example.toycontent.app.common.enumuration.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Transactional Outbox 행. GUARANTEED 등급 알림만 여기를 거친다.
 *
 * <p>도메인 저장과 같은 트랜잭션에서 INSERT되므로 커밋과 함께 살고 롤백과 함께 죽는다.
 * 커밋 직후 즉시 전송이 ack를 받으면 SENT, 실패했거나 파드가 죽어 남으면 PENDING으로 남아
 * {@link NotificationOutboxRelay}가 다시 보낸다.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_NOTIFICATION_OUTBOX",
    indexes = {
        @Index(name = "idx_notification_outbox_status_created", columnList = "status, created_at")
    }
)
public class NotificationOutbox {

  private static final int MAX_ERROR_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_outbox_id")
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 50)
  @Comment("알림 타입")
  private NotificationType type;

  @Column(name = "user_id", nullable = false)
  @Comment("수신자 ID (Kafka 파티션 키)")
  private Long userId;

  @Lob
  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  @Comment("KafkaNotificationDto JSON")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 10)
  @Comment("PENDING / SENT / DEAD")
  private NotificationOutboxStatus status;

  @Column(name = "attempt_count", nullable = false)
  @Comment("전송 시도 횟수 (즉시 전송 포함)")
  private int attemptCount;

  @Column(name = "last_error", length = 500)
  @Comment("마지막 실패 사유")
  private String lastError;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  public static NotificationOutbox pending(NotificationType type, Long userId, String payload) {
    return NotificationOutbox.builder()
        .type(type)
        .userId(userId)
        .payload(payload)
        .status(NotificationOutboxStatus.PENDING)
        .attemptCount(0)
        .build();
  }

  /**
   * 전송 결과를 반영한다. 성공이면 SENT, 실패면 시도 횟수를 올리고 사유를 남긴다.
   * 최대 시도 횟수에 닿으면 DEAD로 내려 릴레이가 더 집지 않게 한다.
   */
  public void settle(Throwable failure, int maxAttempts) {
    this.attemptCount++;
    if (failure == null) {
      this.status = NotificationOutboxStatus.SENT;
      this.sentAt = LocalDateTime.now();
      return;
    }
    String error = String.valueOf(failure);
    this.lastError = error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
    this.status = attemptCount >= maxAttempts ? NotificationOutboxStatus.DEAD : NotificationOutboxStatus.PENDING;
  }

  public boolean isPending() {
    return status == NotificationOutboxStatus.PENDING;
  }
}
