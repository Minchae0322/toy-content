package com.example.toycontent.app.feed.domain;

import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
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

}
