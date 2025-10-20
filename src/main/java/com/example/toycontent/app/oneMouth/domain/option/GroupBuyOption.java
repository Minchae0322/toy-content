package com.example.toycontent.app.oneMouth.domain.option;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.GroupBuyType;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "TB_GROUP_BUY_OPTION")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Comment("공동구매 옵션")
public class GroupBuyOption extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "group_buy_option_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sale_post_id", nullable = false)
  @Comment("판매 게시글")
  private SalePost salePost;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Comment("공동구매 타입")
  private GroupBuyType groupBuyType;

  @Column(nullable = false)
  @Comment("목표 인원")
  private Integer targetCount;

  @ColumnDefault("0")
  @Column(nullable = false)
  @Comment("현재 참여 인원")
  private Integer currentCount;

  @Column(nullable = false)
  @Comment("할인된 가격")
  private Long discountedPrice;

  @Column
  @Comment("정상 가격")
  private Long normalPrice;

  @Column
  @Comment("할인율 (%)")
  private Integer discountRate;

  @Column(nullable = false)
  @Comment("마감 기한")
  private LocalDateTime deadline;

  @Column(length = 500)
  @Comment("초대 토큰 (PRIVATE 타입)")
  private String inviteToken;

  @Column(length = 100)
  @Comment("옵션명")
  private String optionName;

}
