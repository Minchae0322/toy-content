package com.example.toycontent.app.feed.domain;

import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
@Table(name = "TB_FEED", indexes = {
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_product_id", columnList = "product_id")  // 추가
})
public class Feed extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  @Comment("사용자 ID")
  private Long userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(length = 200)
  private String productNameCustom;

  @JoinColumn(name = "category_id")
  @ManyToOne(fetch = FetchType.LAZY)
  @Comment("제품 카테고리 (음료, 스낵, 베이커리 등)")
  private Category category;

  @Column(nullable = false, length = 1000)
  @Comment("리뷰 내용")
  private String review;

  @Column(name = "buy_price")
  @Comment("구매 가격")
  private Integer buyPrice;

  @Column(name = "price")
  @Comment("정가")
  private Integer price;

  @Column(nullable = false)
  @Comment("조회수")
  @NotAudited
  @Builder.Default
  private Integer viewCount = 0;

  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @Comment("상품 이미지 목록")
  private List<FeedAttachmentFile> attachmentFiles = new ArrayList<>();

  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @Comment("피드-해시태그 연결 목록")
  private List<FeedHashtag> hashtags = new ArrayList<>();

  @OneToMany(mappedBy = "feed")
  private List<FeedReaction> reactions;


  /**
   * 피드 정보 업데이트 (첨부파일 제외)
   */
  public void update(FeedRequest.UpdateFeed request, Category category, Product product) {
    this.productNameCustom = request.getProductNameCustom();
    this.review = request.getReview();
    this.buyPrice = request.getBuyPrice();
    this.price = request.getPrice();
    this.category = category;
    this.product = product;
  }

  /**
   * 해시태그 업데이트 (기존 삭제 후 새로 추가)
   */
  public void updateHashtags(List<FeedHashtag> newHashtags) {
    this.hashtags.clear();
    this.hashtags.addAll(newHashtags);
  }

  public void incrementViewCount() {
    this.viewCount++;

  }

  /**
   * 리액션 추가
   */
  public FeedReaction addReaction(Long userId, FeedReactionType reactionType) {
    FeedReaction reaction = FeedReaction.create(this, userId, reactionType);
    this.reactions.add(reaction);
    return reaction;
  }

  /**
   * 리액션 제거
   */
  public void removeReaction(FeedReaction reaction) {
    this.reactions.remove(reaction);
  }

  /**
   * 특정 사용자의 특정 타입 리액션 조회
   */
  public Optional<FeedReaction> findReaction(Long userId, FeedReactionType reactionType) {
    return this.reactions.stream()
        .filter(r -> r.getUserId().equals(userId) && r.getReactionType() == reactionType)
        .findFirst();
  }

  /**
   * 리액션 타입별 개수 조회
   */
  public long getReactionCount(FeedReactionType reactionType) {
    return this.reactions.stream()
        .filter(r -> r.getReactionType() == reactionType)
        .count();
  }

  /**
   * 전체 리액션 개수
   */
  public long getTotalReactionCount() {
    return this.reactions.size();
  }
}
