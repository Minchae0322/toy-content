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
@Table(name = "TB_BITE_SIZE_OPTION")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Comment("한입만 판매 옵션")
public class BiteSizeOption extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "bite_size_option_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_post_id", nullable = false)
  @Comment("판매 게시글")
  private SalePost salePost;

  @Column(nullable = false)
  @Comment("한입 단위 수량 (예: 2개)")
  private Integer unitQuantity;

  @Column(nullable = false)
  @Comment("한입 가격")
  private Long unitPrice;

  @Column(nullable = false)
  @Comment("총 한입 판매 수량")
  private Integer totalBiteCount;

  @ColumnDefault("0")
  @Column(nullable = false)
  @Comment("판매된 한입 수")
  private Integer soldBiteCount;

  @Column
  @Comment("원가 (비교용)")
  private Long originalPrice;

  @Column(length = 100)
  @Comment("옵션명 (선택)")
  private String optionName;

  public void assignSalePost(SalePost salePost) {
    this.salePost = salePost;
  }
}
