package com.example.toycontent.app.reward.exp.domain;

import com.example.toycontent.app.common.enumuration.ExpSource;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "tb_exp_history",
    indexes = {
        @Index(name = "idx_exp_history_user_id", columnList = "user_id"),
        @Index(name = "idx_exp_history_user_source", columnList = "user_id, source"),
        @Index(name = "idx_exp_history_created_at", columnList = "created_at DESC")
    }
)
@Comment("EXP 적립/차감 이력")
public class ExpHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "exp_history_id")
  @Comment("이력 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID")
  private Long userId;

  @Column(name = "amount", nullable = false)
  @Comment("적립/차감 EXP")
  private Long amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 40)
  @Comment("EXP 출처 (FEED_CREATE, BATTLE_VOTE 등)")
  private ExpSource source;

  @Column(name = "source_id")
  @Comment("출처 엔티티 ID (피드 ID, 미션 ID 등)")
  private Long sourceId;

  @Column(name = "result_total_exp", nullable = false)
  @Comment("적립 후 총 누적 EXP (스냅샷)")
  private Long resultTotalExp;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  @Comment("발생 시각")
  private LocalDateTime createdAt;
}
