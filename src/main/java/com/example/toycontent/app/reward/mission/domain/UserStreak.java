package com.example.toycontent.app.reward.mission.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
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
    name = "tb_user_streak",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_streak_user", columnNames = "user_id")
)
@Comment("유저 작성 스트릭 (연속 인증 작성 일수)")
public class UserStreak extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_streak_id")
  @Comment("스트릭 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID")
  private Long userId;

  @Builder.Default
  @Column(name = "current_streak", nullable = false)
  @Comment("현재 연속 일수")
  private Integer currentStreak = 0;

  @Builder.Default
  @Column(name = "max_streak", nullable = false)
  @Comment("역대 최고 연속 일수")
  private Integer maxStreak = 0;

  @Column(name = "last_posted_date")
  @Comment("마지막 인증 작성일")
  private LocalDate lastPostedDate;

  @Builder.Default
  @Column(name = "recovery_tickets", nullable = false)
  @Comment("복구 티켓 보유 수 (하루 놓쳤을 때 사용)")
  private Integer recoveryTickets = 0;

  @Column(name = "last_milestone_reached")
  @Comment("마지막 도달한 마일스톤 (3/7/14/30/100)")
  private Integer lastMilestoneReached;

  public boolean recordPosting(LocalDate today) {
    if (today.equals(this.lastPostedDate)) {
      return false;
    }
    if (this.lastPostedDate != null && this.lastPostedDate.plusDays(1).equals(today)) {
      this.currentStreak++;
    } else {
      this.currentStreak = 1;
    }
    this.lastPostedDate = today;
    if (this.currentStreak > this.maxStreak) {
      this.maxStreak = this.currentStreak;
    }
    return true;
  }

  public void useRecoveryTicket() {
    this.recoveryTickets--;
    this.currentStreak++;
    if (this.currentStreak > this.maxStreak) {
      this.maxStreak = this.currentStreak;
    }
  }

  public void grantRecoveryTickets(int count) {
    this.recoveryTickets += count;
  }
}
