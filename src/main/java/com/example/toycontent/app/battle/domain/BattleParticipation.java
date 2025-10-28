package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.ParticipationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


/**
 * 배틀 참여 기록 엔티티
 *
 * 배틀에 대한 사용자의 참여 이력과 획득 경험치를 관리합니다.
 * - 아이템 등록: +20 EXP
 * - 투표 참여: +5 EXP
 * - 중복 참여 방지 및 리워드 지급 이력 추적용
 */
@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "TB_BATTLE_PARTICIPATION")
@Comment("배틀 참여 기록")
public class BattleParticipation extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_participation_id")
  @Comment("배틀 참여 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_id", nullable = false)
  @Comment("배틀")
  private Battle battle;


  @Column(name = "user_id", nullable = false)
  @Comment("참여 사용자")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("참여 타입 (ITEM_REGISTRATION: 아이템 등록, VOTE: 투표)")
  private ParticipationType type;

  @Builder.Default
  @Column(nullable = false)
  @Comment("획득 경험치")
  private Integer expEarned = 0;

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
   * 아이템 등록 참여 여부 확인
   */
  public boolean isItemRegistration() {
    return type == ParticipationType.ITEM_REGISTRATION;
  }

  /**
   * 투표 참여 여부 확인
   */
  public boolean isVote() {
    return type == ParticipationType.VOTE;
  }

  /**
   * 경험치 획득 여부 확인
   */
  public boolean hasEarnedExp() {
    return expEarned > 0;
  }


}