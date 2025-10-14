package com.example.toycontent.app.Product.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "tb_product_review")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class
ProductReview extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "review_id", nullable = false)
  @Comment("제품 리뷰 고유 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @Comment("제품 정보")
  @NotNull
  private Product product;

  @Column(name = "creator_id", nullable = false)
  @Comment("리뷰 작성자 ID")
  @NotNull
  private Long creatorId;

  @Column(name = "creator_name", nullable = false)
  @Comment("리뷰 작성자 명")
  @NotNull
  private String creatorName;

  @Column(name = "rating", nullable = false)
  @Comment("평점 (1-5)")
  @NotNull
  @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
  @Max(value = 5, message = "평점은 5점 이하여야 합니다")
  private Integer rating;

  @Column(name = "comment", columnDefinition = "TEXT")
  @Comment("리뷰 내용")
  @Size(max = 1000, message = "리뷰 내용은 1000자를 초과할 수 없습니다")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  @Comment("리뷰 상태")
  @Builder.Default
  @NotNull
  private ReviewStatus status = ReviewStatus.ACTIVE;

  @Column(name = "like_count", nullable = false)
  @Comment("좋아요 수")
  @Builder.Default
  @ColumnDefault("0")
  private Integer likeCount = 0;

  @Column(name = "report_count", nullable = false)
  @Comment("신고 수")
  @Builder.Default
  @ColumnDefault("0")
  private Integer reportCount = 0;


}
