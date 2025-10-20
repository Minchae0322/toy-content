package com.example.toycontent.app.oneMouth.domain;

import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.OneMouthStatus;
import com.example.toycontent.app.common.enumuration.SaleType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.SalePostErrorCode;
import com.example.toycontent.app.oneMouth.domain.option.BiteSizeOption;
import com.example.toycontent.app.oneMouth.domain.option.GroupBuyOption;
import com.example.toycontent.app.oneMouth.domain.option.NormalSaleOption;
import com.example.toycontent.app.oneMouth.domain.option.ProxyBuyOption;
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
public class SalePost extends BaseTimeEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    @Comment("게시글 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Comment("연결된 공식 제품 (선택)")
    private Product product;

    @Column(length = 500, nullable = false)
    @Comment("제목")
    private String title;

    @Column(length = 2000)
    @Comment("상품 설명")
    private String description;

    @Column(nullable = false)
    @Comment("판매자 ID")
    private Long sellerId;

    private Long price;

    @Column(length = 100)
    @Comment("판매자 닉네임")
    private String sellerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 20)
    @Comment("판매 방식 (BITE_SIZE/NORMAL/GROUP_BUY/PROXY_BUY)")
    private SaleType saleType;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ON_SALE'")
    @Column(nullable = false, length = 20)
    @Comment("게시글 상태")
    private OneMouthStatus status;

    @Column(length = 255)
    @Comment("거래 희망 지역")
    private String tradeLocation;

    @ColumnDefault("0")
    @Comment("관심 등록 수")
    private Integer favoriteCount;

    @ColumnDefault("0")
    @Comment("조회수")
    private Integer viewCount;

    @ColumnDefault("0")
    @Comment("채팅방 수")
    private Integer chatRoomCount;

    @OneToMany(mappedBy = "salePost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Comment("한입만 옵션 목록")
    private List<BiteSizeOption> biteSizeOptions = new ArrayList<>();

    @OneToMany(mappedBy = "salePost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Comment("일반 판매 옵션 목록")
    private List<NormalSaleOption> normalSaleOptions = new ArrayList<>();

    @OneToMany(mappedBy = "salePost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Comment("공동구매 옵션 목록")
    private List<GroupBuyOption> groupBuyOptions = new ArrayList<>();

    @OneToMany(mappedBy = "salePost", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Comment("대리구매 옵션 목록")
    private List<ProxyBuyOption> proxyBuyOptions = new ArrayList<>();

    @OneToMany(mappedBy = "oneMouth", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @Comment("상품 이미지 목록")
    private List<SalePostAttachmentFile> attachmentFiles = new ArrayList<>();

    public void addBiteSizeOption(BiteSizeOption option) {
        if (this.saleType != SaleType.ONEMOUTH) {
            throw new RestApiException(SalePostErrorCode.SALE_TYPE_MISMATCH);
        }
        this.biteSizeOptions.add(option);
        option.assignSalePost(this);
    }

    public void addNormalSaleOption(NormalSaleOption option) {
        if (this.saleType != SaleType.NORMAL) {
            throw new RestApiException(SalePostErrorCode.SALE_TYPE_MISMATCH);
        }
        this.normalSaleOptions.add(option);
        option.assignSalePost(this);
    }

    public void addGroupBuyOption(GroupBuyOption option) {
        if (this.saleType != SaleType.GROUP_BUY) {
            throw new RestApiException(SalePostErrorCode.SALE_TYPE_MISMATCH);
        }
        this.groupBuyOptions.add(option);
        option.assignSalePost(this);
    }

    public void addProxyBuyOption(ProxyBuyOption option) {
        if (this.saleType != SaleType.PROXY) {
            throw new RestApiException(SalePostErrorCode.SALE_TYPE_MISMATCH);
        }
        this.proxyBuyOptions.add(option);
        option.assignSalePost(this);
    }
}
