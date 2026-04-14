package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.ResultVisibility;
import com.example.toycontent.app.common.enumuration.VoteType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.envers.NotAudited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "TB_BATTLE", indexes = {
        @Index(name = "idx_battle_status", columnList = "status"),
        @Index(name = "idx_battle_hot_score", columnList = "hotScore DESC"),
        @Index(name = "idx_battle_creator", columnList = "creator_id"),
        @Index(name = "idx_battle_category", columnList = "category_id"),
        @Index(name = "idx_battle_status_hot_score", columnList = "status, hotScore DESC"),
        @Index(name = "idx_battle_status_start_date", columnList = "status, startDate"),
        @Index(name = "idx_battle_hot_score_updated", columnList = "hotScoreUpdatedAt"),
        @Index(name = "idx_battle_is_deleted", columnList = "isDeleted")
})
public class Battle extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_id")
  @Comment("배틀 ID")
  private Long id;

  @Column(nullable = false, length = 50)
  @Comment("배틀 제목")
  private String title;

  @Column(length = 500)
  @Comment("배틀 설명")
  private String description;

  @JoinColumn(name = "category_id")
  @ManyToOne(fetch = FetchType.LAZY)
  @Comment("카테고리")
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Comment("아이템 추가 권한 타입")
  private ItemAddPermissionType itemAddPermissionType;

  @Column(name = "creator_id", nullable = false)
  @Comment("생성자 ID")
  private Long creatorId;

  @Column(nullable = false)
  @Comment("시작일")
  private LocalDateTime startDate;

  @Column(nullable = false)
  @Comment("종료일")
  private LocalDateTime endDate;

  @Column(nullable = false)
  @Comment("참여 시작일")
  private LocalDateTime participationStartDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("투표 타입")
  private VoteType voteType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("배틀 상태")
  private BattleStatus status;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 참여자 수")
  private Integer totalParticipants = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 투표 수")
  private Integer totalVotes = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 점수")
  private Integer totalScore = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 댓글 수")
  private Integer totalCommentCount = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 조회 수")
  @NotAudited
  private Integer totalViews = 0;

  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  @Builder.Default
  @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
  @Comment("핫 스코어 (캐시)")
  @NotAudited
  private Double hotScore = 0.0;

  @Column
  @Comment("핫 스코어 마지막 계산 시각")
  @NotAudited
  private LocalDateTime hotScoreUpdatedAt;

  @Builder.Default
  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleItem> items = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleVote> votes = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleParticipation> battleParticipationsList = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BattleAttachmentFile> battleAttachmentFiles = new ArrayList<>();

  public void incrementTotalVotes(int delta) {
    this.totalVotes += delta;
  }

  public void incrementTotalParticipants(int delta) {
    this.totalParticipants += delta;
  }

  /**
   * 핫 스코어 업데이트 (외부 호출용 - 스케줄러)
   */
  public void updateHotScore() {
    this.hotScore = calculateHotScore();
    this.hotScoreUpdatedAt = LocalDateTime.now();
  }

  /**
   * 핫 스코어 계산
   */
  public double calculateHotScore() {
    // 기본 인기도 점수
    double baseScore = (totalVotes * 2.0) + (totalParticipants * 3.0) + (totalViews * 0.1);

    // 시간 가중치 (최근일수록 높은 점수)
    long hoursSinceStart = ChronoUnit.HOURS.between(startDate, LocalDateTime.now());
    double timeDecay = Math.pow(hoursSinceStart + 2, 1.5); // +2는 0으로 나누기 방지

    return baseScore / timeDecay;
  }

  // ========== 투표 타입 판별 ==========

  /** 단일 투표 타입인지 확인 */
  public boolean isSingleVote() {
    return VoteType.SINGLE.equals(this.voteType);
  }

  // ========== 참여자 수 ==========

  /** 새로운 참여자가 투표했을 때 참여자 수 증가 */
  public void incrementTotalParticipants() {
    this.totalParticipants++;
  }

  /** 복수 투표 재투표 시, 기존 참여 기록을 되돌릴 때 참여자 수 감소 */
  public void decrementTotalParticipants() {
    this.totalParticipants = Math.max(0, this.totalParticipants - 1);
  }

  // ========== 투표 수 ==========

  /** 투표 반영 시 투표 수 증가 (단일: 한 표, 복수: 투표한 아이템 수만큼) */
  public void addTotalVotes(int count) {
    this.totalVotes += count;
  }

  /** 복수 투표 재투표 시, 기존 투표 수를 되돌릴 때 감소 */
  public void subtractTotalVotes(int count) {
    this.totalVotes = Math.max(0, this.totalVotes - count);
  }

  // ========== 점수 ==========

  /** 투표 반영 시 총 점수 증가 */
  public void addTotalScore(int score) {
    this.totalScore += score;
  }

  /** 복수 투표 재투표 시, 기존 점수를 되돌릴 때 감소 */
  public void subtractTotalScore(int score) {
    this.totalScore = Math.max(0, this.totalScore - score);
  }

  // ========== 조회 수 ==========

  /** 배틀 상세 조회 시 조회 수 증가 */
  public void incrementTotalViews() {
    this.totalViews++;
  }



  public void incrementTotalCommentCount() {
    this.totalCommentCount++;
  }

  public void decrementTotalCommentCount() {
    this.totalCommentCount = Math.max(0, this.totalCommentCount - 1);
  }
}
