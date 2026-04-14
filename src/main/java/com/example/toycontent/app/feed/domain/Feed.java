package com.example.toycontent.app.feed.domain;

import com.example.toycontent.app.common.enumuration.FeedEvaluation;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "tb_feed", indexes = {
    // 기존 인덱스
    @Index(name = "idx_feed_category", columnList = "category_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_feed_cursor", columnList = "deleted, id DESC"),
    @Index(name = "idx_feed_category_cursor", columnList = "deleted, category_id, id DESC"),
    @Index(name = "idx_feed_user_cursor", columnList = "deleted, user_id, id DESC"),

    // 핫 스코어 정렬용
    @Index(name = "idx_feed_hot_score", columnList = "deleted, hot_score DESC, created_at DESC")
})
public class Feed extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Comment("피드 ID")
  private Long id;

  // ===== 작성자 정보 =====
  @Column(nullable = false)
  @Comment("작성자 ID")
  private Long userId;

  // ===== 제품 정보 =====
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  @Comment("연결된 제품 (선택)")
  private Product product;

  @Column(length = 200, name = "product_name_custom")
  @Comment("직접 입력한 제품명")
  private String productNameCustom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  @Comment("제품 카테고리")
  private Category category;

  // ===== 리뷰 내용 =====
  @Column(nullable = false, length = 1000)
  @Comment("한줄평 (10~100자)")
  private String review;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, name = "evaluation")
  @Comment("제품 평가 (BEST/GOOD/OKAY/BAD)")
  private FeedEvaluation evaluation;

  // ===== 구매 정보 =====
  @Column(length = 100, name = "buy_place")
  @Comment("구매처")
  private String buyPlace;

  @Column(name = "buy_price")
  @Comment("구매 가격")
  private Integer buyPrice;

  @Column(name = "price")
  @Comment("정가")
  private Integer price;

  // ===== 통계 =====
  @Column(nullable = false)
  @Builder.Default
  @Comment("조회수")
  @NotAudited
  private Integer viewCount = 0;

  @Column(nullable = false)
  @Builder.Default
  @Comment("좋아요 수")
  private Integer likeCount = 0;

  @Column(nullable = false)
  @Builder.Default
  @Comment("HOT 스코어")
  private Double hotScore = 0.0;

  @Column(nullable = false)
  @Builder.Default
  @Comment("댓글 수")
  private Integer commentCount = 0;

  @Column(nullable = false)
  @Builder.Default
  @Comment("신고 누적 수")
  private Integer reportCount = 0;

  @Column(name = "quality_bonus_granted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Builder.Default
  @Comment("완성도 보너스 EXP 지급 여부 (중복 지급 방지)")
  private Boolean qualityBonusGranted = false;

  // ===== 트렌딩 =====
  @Column(name = "view_count_24h_ago")
  @Comment("24시간 전 조회수")
  private Integer viewCount24hAgo;

  @Column(name = "is_trending", nullable = false)
  @Builder.Default
  @Comment("트렌딩 여부")
  private Boolean isTrending = false;

  // ===== 삭제 처리 =====
  @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  @Comment("삭제 일시")
  private LocalDateTime deletedAt;

  // ===== 연관 관계 =====
  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  @Comment("첨부 이미지 목록 (최대 5장)")
  private List<FeedAttachmentFile> attachmentFiles = new ArrayList<>();

  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  @Comment("해시태그 목록 (최대 5개)")
  private List<FeedHashtag> hashtags = new ArrayList<>();

  @OneToMany(mappedBy = "feed", fetch = FetchType.LAZY)
  @Builder.Default
  @Comment("리액션 목록")
  private List<FeedReaction> reactions = new ArrayList<>();

  @OneToMany(mappedBy = "feed", fetch = FetchType.LAZY)
  @Builder.Default
  @Comment("피드 스레드")
  private List<FeedThread> threads = new ArrayList<>();

  /**
   * 피드 정보 업데이트 (첨부파일 제외)
   */
  public void update(FeedRequest.UpdateFeed request, Category category, Product product) {
    this.product = product;
    this.productNameCustom = request.getProductNameCustom();
    this.category = category;
    this.review = request.getReview();
    this.evaluation = request.getEvaluation();
    this.buyPlace = request.getBuyPlace();
    this.buyPrice = request.getBuyPrice();
  }

  public void delete() {
    this.isDeleted = true;
    this.deletedAt = LocalDateTime.now();
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
   * 좋아요 수 증가
   */
  public void incrementLikeCount() {
    this.likeCount++;
  }

  /**
   * 좋아요 수 감소
   */
  public void decrementLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }


  /**
   * 핫 수 증가
   */
  public void incrementCommentCount() {
    this.commentCount++;
  }

  /**
   * 핫 수 증가
   */
  public void decrementCommentCount() {
    if (this.commentCount > 0) {
      this.commentCount--;
    }
  }

  /**
   * 리액션 추가 (카운트도 함께 업데이트)
   */
  public FeedReaction addReaction(Long userId, FeedReactionType reactionType) {
    FeedReaction reaction = FeedReaction.create(this, userId, reactionType);
    this.reactions.add(reaction);

    //카운트 증가
    if (reactionType == FeedReactionType.LIKE) {
      incrementLikeCount();
    }

    return reaction;
  }

  /**
   * 리액션 제거 (카운트도 함께 업데이트)
   */
  public void removeReaction(FeedReaction reaction) {
    this.reactions.remove(reaction);

    //카운트 감소
    if (reaction.getReactionType() == FeedReactionType.LIKE) {
      decrementLikeCount();
    }
  }

  /**
   * 트렌딩 여부 계산
   */
  public boolean checkTrending(int threshold) {
    if (viewCount24hAgo == null) return false;
    return (viewCount - viewCount24hAgo) >= threshold;
  }

  /**
   * 트렌딩 상태 갱신
   */
  public void updateTrendingStatus(int threshold) {
    this.isTrending = checkTrending(threshold);
  }

  /**
   * 24시간 전 조회수 스냅샷
   */
  public void snapshotViewCount() {
    this.viewCount24hAgo = this.viewCount;
  }
}
