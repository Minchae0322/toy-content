package com.example.toycontent.app.oneMouth.domain.option;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@Entity
@Table(name = "TB_PROXY_BUY_OPTION")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Comment("대리구매 옵션")
public class ProxyBuyOption extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "proxy_buy_option_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_post_id", nullable = false)
  @Comment("판매 게시글")
  private SalePost salePost;

  @Column(nullable = false)
  @Comment("예상 제품 가격")
  private Long estimatedProductPrice;

  @Column(nullable = false)
  @Comment("대리구매 수수료")
  private Long serviceFee;

  @Column(length = 500, nullable = false)
  @Comment("구매 예정 장소")
  private String purchaseLocation;

  @Column
  @Comment("예상 구매 일시")
  private LocalDateTime expectedPurchaseDate;

  @Column(nullable = false)
  @Comment("최대 대리구매 수량")
  private Integer maxQuantity;

  @ColumnDefault("0")
  @Column(nullable = false)
  @Comment("현재 신청 수량")
  private Integer currentRequestCount;

  @Column(length = 100)
  @Comment("옵션명")
  private String optionName;

  public void assignSalePost(SalePost salePost) {
    this.salePost = salePost;
  }

}
