package com.example.toycontent.app.Product.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.oneMouth.domain.OneMouthAttachmentFile;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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
    @Column(name = "item_id", nullable = false)
    @Comment("제품 고유 ID")
    private Long id;

    @Column(nullable = false, length = 100)
    @Comment("제품명")
    private String name;

    @Column(length = 100)
    @Comment("브랜드명 (예: 스타벅스, 맥도날드, CU)")
    private String brand;

    @Comment("제품 승인 상태 (PENDING: 승인대기, APPROVED: 승인완료, REJECTED: 승인거부)")
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    private ProductStatus status;

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
    @Comment("제품 태그 (세미콜론 구분, 예: '신제품;최신;유행;한정판')")
    private String tags;

    @Comment("조회수 (상세 페이지 방문 횟수)")
    @ColumnDefault("0")
    private Integer viewCount;

    @Comment("찜하기 수 (사용자가 관심상품으로 등록한 횟수)")
    @ColumnDefault("0")
    private Integer likeCount;

    @Comment("제품 등록자 ID (User 테이블 참조)")
    private Long creatorId;

    @Column(nullable = false, updatable = false)
    @Comment("제품 등록 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("제품 정보 수정 일시")
    private LocalDateTime updatedAt;

    @Column
    @Comment("제품 출시일 (브랜드 공식 출시일)")
    private LocalDate releaseDate;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("제품 카테고리 (음료, 스낵, 베이커리 등)")
    private Category category;

    @Column(nullable = false, length = 50)
    @Comment("제품 유형 (SALE: 판매용, RENTAL: 대여용, INFO: 정보제공용)")
    private String productType;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @Comment("제품 첨부 파일 목록 (이미지, 문서 등)")
    private List<ProductAttachmentFile> productAttachmentFiles;

}
