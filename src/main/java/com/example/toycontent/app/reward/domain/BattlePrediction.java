package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "tb_battle_prediction",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_battle_prediction_user_battle",
        columnNames = {"user_id", "battle_id"}
    ),
    indexes = {
        @Index(name = "idx_battle_prediction_user", columnList = "user_id"),
        @Index(name = "idx_battle_prediction_battle", columnList = "battle_id"),
        @Index(name = "idx_battle_prediction_hit_user", columnList = "user_id, hit")
    }
)
@Comment("배틀 예측왕 — 유저가 1순위로 뽑은 아이템 vs 실제 1위")
public class BattlePrediction extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_prediction_id")
  @Comment("배틀 예측 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("예측 유저 ID")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_id", nullable = false)
  @Comment("대상 배틀")
  private Battle battle;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "predicted_item_id", nullable = false)
  @Comment("유저가 1순위로 뽑은 아이템 (BattleVote rank=1 스냅샷)")
  private BattleItem predictedItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "winner_item_id")
  @Comment("실제 우승 아이템 (배틀 종료 시 채워짐)")
  private BattleItem winnerItem;

  @Column(name = "hit")
  @Comment("적중 여부 (null: 배틀 종료 전, true/false: 판정 후)")
  private Boolean hit;

  @Column(name = "settled_at")
  @Comment("적중 판정 일시")
  private LocalDateTime settledAt;
}
