package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.MissionDifficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    name = "tb_daily_mission",
    uniqueConstraints = @UniqueConstraint(name = "uk_daily_mission_code", columnNames = "code")
)
@Comment("일일 미션 정의 마스터")
public class DailyMission extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "daily_mission_id")
  @Comment("미션 ID")
  private Long id;

  @Column(name = "code", nullable = false, length = 60)
  @Comment("미션 코드 (예: PRESS_FIRE_5, WRITE_FEED_WITH_BUY_INFO)")
  private String code;

  @Column(name = "title", nullable = false, length = 120)
  @Comment("미션 제목")
  private String title;

  @Column(name = "description", length = 300)
  @Comment("미션 상세 설명")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "difficulty", nullable = false, length = 20)
  @Comment("난이도 (EASY/NORMAL/HARD)")
  private MissionDifficulty difficulty;

  @Builder.Default
  @Column(name = "target_count", nullable = false)
  @Comment("완료 목표 수 (예: 🔥 5개 누르기 → 5)")
  private Integer targetCount = 1;

  @Column(name = "reward_exp", nullable = false)
  @Comment("완료 시 지급 EXP")
  private Integer rewardExp;

  @Builder.Default
  @Column(name = "grants_gacha_ticket", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Comment("가챠 티켓 지급 여부 (Hard 미션)")
  private Boolean grantsGachaTicket = false;

  @Builder.Default
  @Column(name = "is_fixed_candidate", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Comment("매일 고정 후보 여부 (Hard 중 작성 미션은 항상 후보)")
  private Boolean isFixedCandidate = false;

  @Builder.Default
  @Column(name = "activated", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
  @Comment("활성 여부")
  private Boolean activated = true;
}
