package com.example.toycontent.app.oneMouth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
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
@Table(name = "TB_ONE_MOUTH_FAVORITE")
public class OneMouthFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Comment("관심 게시글 기본 키 ID")
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("관심 등록한 사용자 ID (외부 시스템)")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "one_mouth_id", nullable = false)
    @Comment("관심 등록한 게시글")
    private OneMouth oneMouth;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Comment("관심 등록 일시")
    private LocalDateTime createdAt;
}
