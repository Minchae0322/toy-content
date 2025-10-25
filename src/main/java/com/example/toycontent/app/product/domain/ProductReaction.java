package com.example.toycontent.app.product.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.ReactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "tb_product_reaction",
    indexes = {
        @Index(name = "idx_product_reaction_product", columnList = "product_id"),
        @Index(name = "idx_product_reaction_user", columnList = "user_id"),
        @Index(name = "idx_product_reaction_type", columnList = "reaction_type"),
        @Index(name = "idx_product_reaction_user_product", columnList = "user_id, product_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_product_reaction_type", columnNames = {"user_id", "product_id", "reaction_type"})
    }
)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductReaction extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reaction_id", nullable = false)
  @Comment("제품 반응 고유 ID")
  private Long id;

  @Column(name = "user_id", nullable = false)
  @Comment("반응한 사용자 ID")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  @Comment("반응 대상 제품")
  private Product product;

  @Enumerated(EnumType.STRING)
  @Column(name = "reaction_type", nullable = false, length = 20)
  @Comment("반응 유형 (LIKE: 좋아요, BOOKMARK: 북마크, INTEREST: 관심상품)")
  private ReactionType reactionType;

  @Column(name = "is_active", nullable = false)
  @Comment("활성화 상태 (true: 활성, false: 비활성)")
  @Builder.Default
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Comment("반응 등록 일시")
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  @Comment("반응 수정 일시")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

}
