package com.example.toycontent.app.oneMouth.domain;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.OneMouthStatus;
import com.example.toycontent.app.common.enumuration.Unit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Table(name = "TB_ONE_MOUTH_POST")
public class OneMouth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "one_mouth_id", updatable = false, nullable = false)
    @Comment("게시글 기본 키 ID")
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    @Comment("게시글 제목")
    private String title;

    @Comment("사용자 설정 수량 값")
    @Column(name = "quantity")
    private String quantity;

    @Comment("유닛 (개, 입, 그램, 커스텀)")
    @Enumerated(EnumType.STRING)
    private Unit unit;

    @Comment("가격")
    @Column(name = "price")
    private Long price;

    @Comment("판매 상태 (판매중, 품절, 예약중, 판매중단)")
    @Enumerated(EnumType.STRING)
    private OneMouthStatus oneMouthStatus;

    @Column(length = 4000)
    @Comment("제품 상세 설명")
    private String description;

    @Column(nullable = false)
    @Comment("판매자 아이디")
    private Long sellerId;

    @Column(nullable = false)
    @Comment("판매자 거래 위치")
    private String location;

    @Column(nullable = false, updatable = false)
    @Comment("생성 시간")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("수정 시간")
    private LocalDateTime updatedAt;

    @Column(name = "hits", columnDefinition = "Integer default 0")
    @Comment("조회수")
    private Integer hits;

    @Column(name = "thumbnail_url", length = 2000)
    @Comment("대표 이미지 (썸네일) URL")
    private String thumbnailUrl;

    @Column(name = "thumbnail_file_id")
    @Comment("대표 이미지 파일 ID (파일서비스 참조)")
    private Long thumbnailFileId;

    @OneToMany(mappedBy = "oneMouth", fetch = FetchType.LAZY)
    @Comment("관심")
    private List<OneMouthFavorite> favorites;

    @OneToMany(mappedBy = "oneMouth", fetch = FetchType.LAZY)
    private List<OneMouthAttachmentFile> oneMouthAttachmentFiles;
}
