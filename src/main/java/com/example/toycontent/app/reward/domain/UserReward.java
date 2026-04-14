package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
    name = "tb_user_reward",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_reward_user", columnNames = "user_id"),
    indexes = {
        @Index(name = "idx_user_reward_total_exp", columnList = "total_exp DESC"),
        @Index(name = "idx_user_reward_level", columnList = "level DESC")
    }
)
@Comment("유저별 EXP/레벨 집계 (1 user : 1 row)")
public class UserReward extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_reward_id")
  @Comment("유저 보상 집계 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID (auth-service FK)")
  private Long userId;

  @Builder.Default
  @Column(name = "total_exp", nullable = false)
  @Comment("누적 총 EXP (역사적 총합)")
  private Long totalExp = 0L;

  @Builder.Default
  @Column(name = "level", nullable = false)
  @Comment("현재 레벨")
  private Integer level = 1;

  @Builder.Default
  @Column(name = "current_level_exp", nullable = false)
  @Comment("현재 레벨에서 쌓인 EXP")
  private Long currentLevelExp = 0L;

  @Builder.Default
  @Column(name = "next_level_exp", nullable = false)
  @Comment("다음 레벨 도달에 필요한 EXP")
  private Long nextLevelExp = 100L;

  @Builder.Default
  @Column(name = "season_exp", nullable = false)
  @Comment("현재 시즌 EXP")
  private Long seasonExp = 0L;

  @Column(name = "season_code", length = 20)
  @Comment("현재 시즌 코드 (예: 2026-Q2)")
  private String seasonCode;

  public void addExp(long amount) {
    this.totalExp += amount;
    this.currentLevelExp += amount;
    this.seasonExp += amount;
    checkLevelUp();
  }

  public void addSeasonExp(long amount, String seasonCode) {
    if (!seasonCode.equals(this.seasonCode)) {
      this.seasonCode = seasonCode;
      this.seasonExp = 0L;
    }
    this.seasonExp += amount;
  }

  private void checkLevelUp() {
    while (this.currentLevelExp >= this.nextLevelExp) {
      this.currentLevelExp -= this.nextLevelExp;
      this.level++;
      this.nextLevelExp = calculateNextLevelExp(this.level);
    }
  }

  private long calculateNextLevelExp(int level) {
    return 100L * level;
  }
}
