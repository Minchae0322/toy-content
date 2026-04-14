package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.MissionProgressStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
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
    name = "tb_user_daily_mission_assignment",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_daily_mission_date",
        columnNames = {"user_id", "mission_id", "assigned_date"}
    ),
    indexes = {
        @Index(name = "idx_user_daily_mission_user_date", columnList = "user_id, assigned_date DESC")
    }
)
@Comment("유저별 오늘 할당된 미션 & 진행 상태")
public class UserDailyMissionAssignment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_daily_mission_assignment_id")
  @Comment("유저 일일 미션 할당 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mission_id", nullable = false)
  @Comment("할당된 미션")
  private DailyMission mission;

  @Column(name = "assigned_date", nullable = false)
  @Comment("할당 날짜")
  private LocalDate assignedDate;

  @Builder.Default
  @Column(name = "current_count", nullable = false)
  @Comment("현재 진행 수")
  private Integer currentCount = 0;

  @Column(name = "target_count", nullable = false)
  @Comment("목표 수 (스냅샷)")
  private Integer targetCount;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "status", nullable = false, length = 20)
  @Comment("진행 상태")
  private MissionProgressStatus status = MissionProgressStatus.IN_PROGRESS;

  @Column(name = "completed_at")
  @Comment("완료 일시")
  private LocalDateTime completedAt;

  @Column(name = "claimed_at")
  @Comment("보상 수령 일시")
  private LocalDateTime claimedAt;

  public boolean incrementProgress(int amount) {
    if (this.status != MissionProgressStatus.IN_PROGRESS) {
      return false;
    }
    this.currentCount = Math.min(this.currentCount + amount, this.targetCount);
    if (this.currentCount >= this.targetCount) {
      this.status = MissionProgressStatus.COMPLETED;
      this.completedAt = LocalDateTime.now();
      return true;
    }
    return false;
  }

  public void claim() {
    this.status = MissionProgressStatus.CLAIMED;
    this.claimedAt = LocalDateTime.now();
  }
}
