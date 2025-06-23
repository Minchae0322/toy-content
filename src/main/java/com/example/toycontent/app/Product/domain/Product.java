package com.example.toycontent.app.Product.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
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

    @Column(length = 1000)
    @Comment("제품 상세 설명")
    private String description;

    @Column(nullable = false)
    @Comment("판매자 아이디")
    private Long sellerId;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("카테고리")
    private Category category;

    @Column(nullable = false)
    @Comment("거래 단위 (예: piece, gram, hour 등)")
    private String unitType;

    @Column(nullable = false)
    @Comment("남은 수량")
    private Integer availableQuantity;

    @Column(nullable = false)
    @Comment("판매자 또는 거래 위치")
    private String location;

    @Column(nullable = false, updatable = false)
    @Comment("레코드 생성 시간")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("레코드 수정 시간")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Comment("제품 유형 (예: rental, sale)")
    private String productType;
}
