package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "TB_BATTLE_VOTE",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_battle_user_rank",
        columnNames = {"battle_id", "user_id", "rank"}
    )
)
@Comment("배틀 투표")
public class BattleVote extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_vote_id")
  @Comment("배틀 투표 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_id", nullable = false)
  @Comment("배틀")
  private Battle battle;

  @Column(name = "user_id", nullable = false)
  @Comment("투표 사용자")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_item_id", nullable = false)
  @Comment("투표 대상 아이템")
  private BattleItem battleItem;

  @Builder.Default
  @Column(nullable = false)
  @Comment("순위 (1위/2위/3위, MULTIPLE 투표 타입용)")
  private Integer rank = 1;

  @Builder.Default
  @Column(nullable = false)
  @Comment("점수 (1위=3점, 2위=2점, 3위=1점)")
  private Integer score = 1;

  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  // ========== 비즈니스 메서드 ==========

  /**
   * Soft Delete
   */
  public void softDelete() {
    this.isDeleted = true;
  }

  /**
   * 1위 투표 여부 확인
   */
  public boolean isFirstPlace() {
    return rank == 1;
  }

  /**
   * 유효한 투표 여부 확인
   */
  public boolean isValid() {
    return !isDeleted && rank >= 1 && rank <= 3;
  }
}