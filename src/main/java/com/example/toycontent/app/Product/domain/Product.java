package com.example.toycontent.app.Product.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.oneMouth.domain.OneMouthAttachmentFile;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
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
@Table(name = "tb_product")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false)
    private Long id;

    @Column(nullable = false)
    @Comment("이름")
    private String name;

    @Column(length = 100)
    @Comment("브랜드명")
    private String brand;

    @Column(length = 1000)
    @Comment("제품 상세 설명")
    private String description;

    @Column(length = 1000)
    @Comment("제품 판매처")
    private String distributor;

    @Column(length = 1000)
    @Comment("제품 가격")
    private String price;

    @Column(length = 1000)
    @Comment("제품 특징")
    private String feature;

    @Column(length = 1000)
    @Comment("제품 태그 (신제품;최신;유행으로 구성)")
    private String tags;

    @Column
    @Comment("출시일")
    private LocalDate releaseDate;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("카테고리")
    private Category category;

    @Column(nullable = false, updatable = false)
    @Comment("생성 시간")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정 시간")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Comment("제품 유형 (예: rental, sale)")
    private String productType;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductAttachmentFile> productAttachmentFiles;

}
