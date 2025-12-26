package com.example.toycontent.app.battle.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.product.domain.Product;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "TB_BATTLE_ITEM")
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

  /**
   * 투표 수 증가
   */
  public void incrementVote() {
    this.voteCount++;
  }


  /**
   * 점수 추가
   * - SINGLE 투표: 1점
   * - MULTIPLE 투표: 1위=3점, 2위=2점, 3위=1점
   * @param score 추가할 점수
   */
  public void addScore(int score) {
    this.totalScore += score;
  }

  /**
   * 점수 차감 (투표 취소 시)
   * @param score 차감할 점수
   */
  public void subtractScore(int score) {
    this.totalScore = Math.max(0, this.totalScore - score);
  }

  /**
   * 신고 수 증가
   * 3회 이상 신고 시 자동으로 검토중 상태로 변경
   */
  public void incrementReport() {
    this.reportCount++;
    if (this.reportCount >= 3) {
      this.status = BattleItemStatus.UNDER_REVIEW;
    }
  }

  /**
   * 아이템 제외 처리
   * 배틀에서 제외되며 투표 불가
   */
  public void exclude() {
    this.status = BattleItemStatus.EXCLUDED;
  }

  /**
   * 아이템 승인 처리
   * 검토 완료 후 다시 활성화
   */
  public void approve() {
    this.status = BattleItemStatus.ACTIVE;
  }

  /**
   * Soft Delete
   */
  public void softDelete() {
    this.isDeleted = true;
  }

  // ========== Helper 메서드 ==========

  /**
   * 제품명 조회
   * Product가 있으면 Product 정보, 없으면 커스텀 정보 반환
   */
  public String getName() {
    return product != null ? product.getName() : customName;
  }

  /**
   * 브랜드명 조회
   * Product가 있으면 Product 정보, 없으면 커스텀 정보 반환
   */
  public String getBrand() {
    return product != null ? product.getBrand() : customBrand;
  }

  /**
   * 커스텀 아이템 여부 확인
   */
  public boolean isCustomItem() {
    return product == null;
  }


  /**
   * 활성 상태 여부 확인
   */
  public boolean isActive() {
    return status == BattleItemStatus.ACTIVE && !isDeleted;
  }

  /**
   * 투표 가능 여부 확인
   */
  public boolean canVote() {
    return isActive() && status == BattleItemStatus.ACTIVE;
  }

  // BattleItem.java
  public void incrementVoteCount(int delta) {
    this.voteCount += delta;
  }
}