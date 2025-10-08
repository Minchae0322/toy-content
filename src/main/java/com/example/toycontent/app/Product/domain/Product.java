package com.example.toycontent.app.Product.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Table(name = "tb_product", indexes = {
    @Index(name = "idx_product_status", columnList = "status"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_name", columnList = "name"),
    @Index(name = "idx_product_brand", columnList = "brand")
})
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    @Comment("제품 고유 ID")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    @Comment("제품명")
    private String name;

    @Column(name = "brand", length = 100)
    @Comment("브랜드명 (예: 스타벅스, 맥도날드, CU)")
    private String brand;

    @Comment("제품 승인 상태 (PENDING: 승인대기, APPROVED: 승인완료, REJECTED: 승인거부)")
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Builder.Default
    @Column(name = "status")
    private ProductStatus status = ProductStatus.PENDING;

    @Column(length = 1000)
    @Comment("제품 상세 설명 (맛, 특징, 용량 등)")
    private String description;

    @Column(length = 1000)
    @Comment("제품 판매처 (편의점, 마트, 온라인몰 등)")
    private String distributor;

    @Column(length = 100)
    @Comment("제품 정가 (원 단위, 예: 3500)")
    private String price;

    @Column(length = 1000)
    @Comment("제품 주요 특징 (단맛, 매운맛, 한정판매 등)")
    private String feature;

    @Column(length = 1000)
    @Comment("제품 태그 (세미콜론 구분)")
    private String tags;

    @Column()
    @ColumnDefault("0.0")
    @Comment("평균 평점")
    private Double avgRating;

    @Comment("조회수 (상세 페이지 방문 횟수)")
    @ColumnDefault("0")
    private Integer viewCount;

    @Comment("찜하기 수 (사용자가 관심상품으로 등록한 횟수)")
    @ColumnDefault("0")
    private Integer likeCount;

    @Column
    @ColumnDefault("0")
    @Comment("총 공유 횟수 (SNS, 링크 공유)")
    private Integer shareCount;

    @Comment("제품 등록자 ID (User 테이블 참조)")
    private Long creatorId;

    @Column
    @Comment("제품 출시일 (브랜드 공식 출시일)")
    private LocalDate releaseDate;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("제품 카테고리 (음료, 스낵, 베이커리 등)")
    private Category category;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @Comment("제품 거래 게시물 목록")
    private List<OneMouth> oneMouths;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("제품 첨부 파일 목록 (이미지, 문서 등)")
    private List<ProductAttachmentFile> productAttachmentFiles;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductReview> productReviews;

}


