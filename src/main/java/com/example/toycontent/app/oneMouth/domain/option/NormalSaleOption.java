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
@Table(name = "TB_NORMAL_SALE_OPTION")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Comment("일반 판매 옵션")
public class NormalSaleOption extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "normal_sale_option_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_post_id", nullable = false)
  @Comment("판매 게시글")
  private SalePost salePost;

  @Column(nullable = false)
  @Comment("판매 가격")
  private Long price;

  @Column
  @Comment("원가")
  private Long originalPrice;

  @Column(nullable = false)
  @Comment("총 재고")
  private Integer totalStock;

  @ColumnDefault("0")
  @Column(nullable = false)
  @Comment("판매된 수량")
  private Integer soldCount;

  @Column(length = 100)
  @Comment("옵션명 (예: 5개입, 10개입)")
  private String optionName;

  public void assignSalePost(SalePost salePost) {
    this.salePost = salePost;
  }

}
