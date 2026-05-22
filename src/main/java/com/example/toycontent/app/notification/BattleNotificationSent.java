package com.example.toycontent.app.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_BATTLE_NOTIFICATION_SENT",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_battle_notification_sent",
            columnNames = {"battle_id", "phase", "user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_battle_notification_sent_battle_phase", columnList = "battle_id, phase")
    }
)
public class BattleNotificationSent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_notification_sent_id")
  private Long id;

  @Column(name = "battle_id", nullable = false)
  @Comment("배틀 ID")
  private Long battleId;

  @Enumerated(EnumType.STRING)
  @Column(name = "phase", nullable = false, length = 10)
  @Comment("알림 단계 (D7, END)")
  private BattleNotificationPhase phase;

  @Column(name = "user_id", nullable = false)
  @Comment("수신자 ID")
  private Long userId;

  @CreatedDate
  @Column(name = "sent_at", nullable = false, updatable = false)
  private LocalDateTime sentAt;

  public static BattleNotificationSent of(Long battleId, BattleNotificationPhase phase, Long userId) {
    return BattleNotificationSent.builder()
        .battleId(battleId)
        .phase(phase)
        .userId(userId)
        .build();
  }
}
