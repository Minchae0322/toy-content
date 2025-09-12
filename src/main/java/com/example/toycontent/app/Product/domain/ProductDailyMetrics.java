package com.example.toycontent.app.Product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Builder
@Entity
@Table(name = "tb_product_daily_metrics", indexes = {
    @Index(name = "idx_daily_metrics_product_date", columnList = "product_id,metricsDate"),
    @Index(name = "idx_daily_metrics_date", columnList = "metricsDate"),
    @Index(name = "uk_product_date", columnList = "productId,metricsDate", unique = true)
})
@NoArgsConstructor
@AllArgsConstructor
public class ProductDailyMetrics {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", insertable = false, updatable = false)
  private Product product;

  @Column(nullable = false)
  @Comment("집계 날짜")
  private LocalDate metricsDate;

  @Column(nullable = false)
  @ColumnDefault("0")
  @Comment("당일 조회수")
  private Integer dailyViews;

  @Column(nullable = false)
  @ColumnDefault("0")
  @Comment("당일 좋아요 수")
  private Integer dailyLikes;

  @Column(nullable = false)
  @ColumnDefault("0")
  @Comment("당일 거래 수")
  private Integer dailyOrders;

  @Column(nullable = false)
  @ColumnDefault("0")
  @Comment("당일 공유 수")
  private Integer dailyShares;

  @Column(nullable = false)
  @ColumnDefault("0")
  @Comment("당일 리뷰 수")
  private Integer dailyReviews;

  @Column()
  @ColumnDefault("0.0")
  @Comment("당일 평균 평점")
  private Double dailyAvgRating;

  @Column(nullable = false)
  @Comment("마지막 업데이트 시간")
  private LocalDateTime lastUpdatedAt;

}
