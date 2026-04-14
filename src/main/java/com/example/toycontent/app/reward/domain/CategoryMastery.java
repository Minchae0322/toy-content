package com.example.toycontent.app.reward.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.CategoryMasteryTier;
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
    name = "tb_category_mastery",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_category_mastery_user_category",
        columnNames = {"user_id", "category_id"}
    ),
    indexes = {
        @Index(name = "idx_category_mastery_user", columnList = "user_id"),
        @Index(name = "idx_category_mastery_category_tier", columnList = "category_id, tier")
    }
)
@Comment("유저 × 카테고리 전문성 트랙 (관심자/애호가/큐레이터/전문가)")
public class CategoryMastery extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "category_mastery_id")
  @Comment("카테고리 마스터리 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("유저 ID")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  @Comment("카테고리")
  private Category category;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "tier", nullable = false, length = 20)
  @Comment("현재 등급")
  private CategoryMasteryTier tier = CategoryMasteryTier.NONE;

  @Builder.Default
  @Column(name = "feed_count", nullable = false)
  @Comment("해당 카테고리 인증 글 수")
  private Integer feedCount = 0;

  @Builder.Default
  @Column(name = "battle_vote_count", nullable = false)
  @Comment("해당 카테고리 배틀 투표 수")
  private Integer battleVoteCount = 0;

  @Builder.Default
  @Column(name = "pick_comment_count", nullable = false)
  @Comment("해당 카테고리 PICK 한마디 작성 수")
  private Integer pickCommentCount = 0;

  @Builder.Default
  @Column(name = "prediction_total_count", nullable = false)
  @Comment("예측 시도 총 수")
  private Integer predictionTotalCount = 0;

  @Builder.Default
  @Column(name = "prediction_hit_count", nullable = false)
  @Comment("예측 적중 수")
  private Integer predictionHitCount = 0;

  @Builder.Default
  @Column(name = "prediction_accuracy", nullable = false)
  @Comment("예측 정확도 (0.00 ~ 1.00)")
  private Double predictionAccuracy = 0.0;

  @Builder.Default
  @Column(name = "monthly_prediction_king_count", nullable = false)
  @Comment("월간 예측왕 달성 횟수")
  private Integer monthlyPredictionKingCount = 0;
}
