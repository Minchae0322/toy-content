package com.example.toycontent.app.reward.exp.domain;

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
    name = "tb_daily_exp_cap",
    uniqueConstraints = @UniqueConstraint(name = "uk_daily_exp_cap_user_date", columnNames = {"user_id", "cap_date"})
)
@Comment("유저 일일 EXP 캡 (하루 최대 지급량 추적)")
public class DailyExpCap extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_exp_cap_id")
  @Comment("일일 캡 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID")
  private Long userId;

  @Column(name = "cap_date", nullable = false)
  @Comment("캡 적용 날짜 (KST 기준)")
  private LocalDate capDate;

  @Builder.Default
  @Column(name = "used_amount", nullable = false)
  @Comment("오늘 사용한 EXP")
  private Long usedAmount = 0L;

  /**
   * 일일 한도 내에서 실제 지급 가능한 EXP를 계산하고 사용량을 증가시킨다.
   *
   * @param amount     지급 요청 EXP
   * @param dailyLimit 일일 한도
   * @return 실제 지급 가능한 EXP (0이면 캡 초과)
   */
  public long consume(long amount, long dailyLimit) {
    long remaining = dailyLimit - this.usedAmount;
    if (remaining <= 0) {
      return 0;
    }
    long actual = Math.min(amount, remaining);
    this.usedAmount += actual;
    return actual;
  }
}
