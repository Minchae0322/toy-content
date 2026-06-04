package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.product.domain.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "TB_BATTLE_ITEM", indexes = {
        @Index(name = "idx_battle_item_battle", columnList = "battle_id"),
        @Index(name = "idx_battle_item_product", columnList = "product_id"),
        @Index(name = "idx_battle_item_status", columnList = "status"),
        @Index(name = "idx_battle_item_battle_status", columnList = "battle_id, status"),
        @Index(name = "idx_battle_item_registered_by", columnList = "registered_by"),
        @Index(name = "idx_battle_item_type", columnList = "itemType")
})
public class BattleItem extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_item_id")
  @Comment("배틀 아이템 ID")
  private Long id;


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_id", nullable = true)
  @Comment("배틀")
  private Battle battle;

  // ========== 타입 구분 ==========

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("아이템 타입 (PRODUCT: 제품, CUSTOM: 사용자 직접입력, YOUTUBE: 유튜브)")
  private BattleItemType itemType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  @Comment("제품 (null이면 커스텀 아이템)")
  private Product product;

  // ========== 커스텀 아이템 정보 (product가 null인 경우) ==========

  @Column(length = 30)
  @Comment("커스텀 제품명")
  private String customName;

  @Column(length = 20)
  @Comment("커스텀 브랜드")
  private String customBrand;

  @Column(length = 500)
  @Comment("커스텀 이미지 URL")
  private String customImageUrl;

  // ========== 외부 콘텐츠 공통 (YOUTUBE, 향후 TIKTOK 등) ==========

  @Column(length = 500)
  @Comment("외부 콘텐츠 원본 URL")
  private String contentUrl;

  @Column(length = 50)
  @Comment("외부 콘텐츠 고유 ID (YouTube videoId 등)")
  private String contentId;

  // ========== 등록 정보 ==========

  @Column(name = "registered_by")
  @Comment("등록자 ID (오픈 배틀의 경우)")
  private Long registerId;

  @Builder.Default
  @Column(nullable = false)
  @Comment("표시 순서")
  private Integer displayOrder = 0;

  // ========== 상태 관리 ==========

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("아이템 상태 (ACTIVE: 활성, UNDER_REVIEW: 검토중, EXCLUDED: 제외됨)")
  private BattleItemStatus status;

  // ========== 통계 ==========

  @Builder.Default
  @Column(nullable = false)
  @Comment("투표 수")
  private Integer voteCount = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("총 점수 (SINGLE: 투표당 1점, MULTIPLE: 1위=3점, 2위=2점, 3위=1점)")
  private Integer totalScore = 0;

  // ========== SWIPE 통계 (VoteType.SWIPE 전용, vote 누적과 별도) ==========

  @Builder.Default
  @Column(name = "strong_pick_count", nullable = false, columnDefinition = "INT DEFAULT 0")
  @Comment("강추 PICK 누적 수")
  private Integer strongPickCount = 0;

  @Builder.Default
  @Column(name = "pick_count", nullable = false, columnDefinition = "INT DEFAULT 0")
  @Comment("PICK 누적 수")
  private Integer pickCount = 0;

  @Builder.Default
  @Column(name = "pass_count", nullable = false, columnDefinition = "INT DEFAULT 0")
  @Comment("PASS 누적 수")
  private Integer passCount = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("신고 수 (3회 이상 시 자동 검토중 상태)")
  private Integer reportCount = 0;

  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;


  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "battleItem")
  private List<BattleVote> battleVotes = new ArrayList<>();

  // ========== 투표 수 ==========

  /** 투표 반영 시 득표 수 증가 */
  public void incrementVoteCount() {
    this.voteCount++;
  }

  /** 복수 투표 재투표 시, 기존 득표를 되돌릴 때 감소 */
  public void decrementVoteCount() {
    this.voteCount = Math.max(0, this.voteCount - 1);
  }

  // ========== 점수 ==========

  /**
   * 투표 반영 시 점수 증가
   * - SINGLE 투표: 한 표당 고정 점수
   * - MULTIPLE 투표: 순위에 따라 차등 점수
   */
  public void addScore(int score) {
    this.totalScore += score;
  }

  /** 복수 투표 재투표 시, 기존 점수를 되돌릴 때 감소 */
  public void subtractScore(int score) {
    this.totalScore = Math.max(0, this.totalScore - score);
  }

  // ========== SWIPE 카운터 ==========

  public void incrementStrongPickCount() {
    this.strongPickCount++;
  }

  public void incrementPickCount() {
    this.pickCount++;
  }

  public void incrementPassCount() {
    this.passCount++;
  }

  /** 멱등 덮어쓰기 정책에서 기존 verdict 카운터를 되돌릴 때 사용. 음수 방지. */
  public void decrementStrongPickCount() {
    this.strongPickCount = Math.max(0, this.strongPickCount - 1);
  }

  public void decrementPickCount() {
    this.pickCount = Math.max(0, this.pickCount - 1);
  }

  public void decrementPassCount() {
    this.passCount = Math.max(0, this.passCount - 1);
  }

  /** SWIPE 랭킹 점수: STRONG_PICK * 3 + PICK * 1 (PASS는 점수 미반영) */
  public int getSwipeRankingScore() {
    return strongPickCount * 3 + pickCount;
  }

  // ========== 상태 변경 ==========

  /**
   * 신고 접수 시 신고 수 증가
   * 누적 신고가 기준치 이상이면 자동으로 검토 대기 상태로 전환
   */
  public void incrementReport() {
    this.reportCount++;
    if (this.reportCount >= 3) {
      this.status = BattleItemStatus.UNDER_REVIEW;
    }
  }

  /** 관리자 검토 후 아이템 제외 처리 (투표 불가) */
  public void exclude() {
    this.status = BattleItemStatus.EXCLUDED;
  }

  /** 관리자 검토 완료 후 다시 활성화 */
  public void approve() {
    this.status = BattleItemStatus.ACTIVE;
  }

  /** 논리 삭제 */
  public void softDelete() {
    this.isDeleted = true;
  }

  // ========== 조회 헬퍼 ==========

  // ========== 조회 헬퍼 ==========


  /**
   * 타입에 관계없이 표시할 이름을 반환한다.
   * - PRODUCT: 연결된 제품의 이름
   * - CUSTOM: 사용자가 직접 입력한 커스텀 제품명
   * - YOUTUBE: 외부 콘텐츠 원본 URL
   */
  public String getDisplayName() {
    return switch (itemType) {
      case PRODUCT -> product != null ? product.getName() : "알 수 없는 제품";
      case CUSTOM, YOUTUBE -> customName;
    };
  }

  /**
   * 타입에 관계없이 표시할 이미지 URL을 반환한다.
   * - PRODUCT: null (AttachmentFile 기반이므로 DTO 변환 시 별도 처리)
   * - CUSTOM: 사용자가 등록한 이미지 URL
   * - YOUTUBE: videoId 기반 YouTube 썸네일 URL (480x360)
   */
  public String getDisplayImageUrl() {
    return switch (itemType) {
      case PRODUCT -> null;
      case CUSTOM -> customImageUrl;
      case YOUTUBE -> contentId != null
          ? "https://img.youtube.com/vi/" + contentId + "/hqdefault.jpg"
          : null;
    };
  }

  public String getEmbedUrl() {
    if (itemType == BattleItemType.YOUTUBE && contentId != null) {
      return "https://www.youtube.com/embed/" + contentId;
    }
    return null;
  }


  /** 활성 상태이며 삭제되지 않은 아이템인지 확인 */
  public boolean isActive() {
    return status == BattleItemStatus.ACTIVE && !isDeleted;
  }

  /** 투표 가능 여부 (활성 상태일 때만 투표 가능) */
  public boolean canVote() {
    return isActive();
  }
}

