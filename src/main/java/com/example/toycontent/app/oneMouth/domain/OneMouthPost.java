package com.example.toycontent.app.oneMouth.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseEntity;
import com.example.toycontent.app.common.enumuration.Unit;
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
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "TB_ONE_MOUTH_POST")
public class OneMouthPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Comment("게시글 기본 키 ID")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    @Comment("게시글 제목")
    private String title;

    @Column(name = "content", columnDefinition = "CLOB", nullable = false)
    @Comment("게시글 내용")
    private String content;

    @Comment("커스텀 설정 시, 사용자 설정 수량 값")
    @Column(name = "quantity")
    private String quantity;

    @Enumerated(EnumType.STRING)
    private Unit unit;

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
