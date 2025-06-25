package com.example.toycontent.app.oneMouth.domain;


import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.Unit;
import jakarta.persistence.*;
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
import java.util.List;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tb_one_mouth_draft")
public class OneMouthDraft extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "one_mouth_draft_id", updatable = false, nullable = false)
    @Comment("임시저장 게시글 기본 키 ID")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    @Comment("게시글 제목")
    private String title;

    @Column(length = 4000)
    @Comment("제품 상세 설명")
    private String description;

    @Comment("사용자 설정 수량 값")
    @Column(name = "quantity")
    private String quantity;

    @Comment("유닛 (개, 입, 그램, 커스텀)")
    @Enumerated(EnumType.STRING)
    private Unit unit;

    @Comment("가격")
    @Column(name = "price")
    private Long price;

    @JoinColumn(name = "category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @Comment("카테고리")
    private Category category;

    @Column(nullable = false)
    @Comment("판매자 아이디")
    private Long sellerId;

    @Column(nullable = false, updatable = false)
    @Comment("생성 시간")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정 시간")
    private LocalDateTime updatedAt;

    @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "oneMouthDraft")
    private List<OneMouthDraftAttachmentFile> oneMouthDraftAttachmentFiles;

    public OneMouthDraft update() {
        return this;
    }
}
