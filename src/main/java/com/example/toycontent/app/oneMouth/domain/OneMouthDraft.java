package com.example.toycontent.app.oneMouth.domain;


import com.example.toycontent.app.category.domain.Category;
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
import java.util.List;

@Getter
@SuperBuilder
@DynamicInsert
@DynamicUpdate
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "TB_ONE_MOUTH_DRAFT")
public class OneMouthDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Comment("임시저장 게시글 기본 키 ID")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    @Comment("게시글 제목")
    private String title;

    @Column(name = "content", columnDefinition = "CLOB", nullable = false)
    @Comment("게시글 내용")
    private String content;

    @Comment("사용자 설정 수량 값")
    @Column(name = "quantity")
    private String quantity;

    @Comment("유닛 (개, 입, 그램, 커스텀)")
    @Enumerated(EnumType.STRING)
    private Unit unit;

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

    @OneToMany
    @JoinColumn(name = "one_mout_attachment_file_id")
    private List<OneMouthAttachmentFile> oneMouthAttachmentFiles;

    public OneMouthDraft update() {
        return this;
    }
}
