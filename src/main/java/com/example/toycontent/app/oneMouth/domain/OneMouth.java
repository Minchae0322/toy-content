package com.example.toycontent.app.oneMouth.domain;

import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.OneMouthStatus;
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
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "TB_ONE_MOUTH_POST")
public class OneMouth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "one_mouth_id", updatable = false, nullable = false)
    @Comment("개인 거래 게시글 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Comment("연결된 공식 제품")
    private Product product;

    @Column(length = 500, nullable = false)
    @Comment("제목")
    private String title;

    @Column(length = 1000)
    @Comment("추가 설명 (판매 이유 등)")
    private String description;

    @Comment("판매자 정보")
    private Long sellerId;

    private String sellerName;

    @Column(nullable = false)
    @Comment("판매 가격")
    private Long sellingPrice;

    @Column(length = 255)
    @Comment("거래 희망 지역 (직거래 시)")
    private String tradeLocation;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("ON_SALE")
    @Column(nullable = false, length = 20)
    @Comment("거래 상태 (판매중, 예약중, 완료)")
    private OneMouthStatus oneMouthStatus;

    @ColumnDefault("0")
    @Comment("관심 등록 수")
    private Integer favoriteCount;

    @Comment("조회수 (상세 페이지 방문 횟수)")
    @ColumnDefault("0")
    private Integer viewCount;

    @ColumnDefault("0")
    @Comment("채팅방 수")
    private Integer chatRoomCount;

    @OneToMany(mappedBy = "oneMouth", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("상품 이미지 목록")
    private List<OneMouthAttachmentFile> attachmentFiles = new ArrayList<>();
}
