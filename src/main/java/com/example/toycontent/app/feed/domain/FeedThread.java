package com.example.toycontent.app.feed.domain;


import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.FeedEvaluation;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "tb_feed_thread")
public class FeedThread extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Comment("피드 스레드 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id")
  @Comment("피드 아이디")
  private Feed feed;


  @Column(nullable = false)
  @Comment("작성자 ID")
  private Long userId;

  @Column(nullable = false, length = 1000)
  @Comment("한줄평 (10~100자)")
  private String review;

  @Enumerated(EnumType.STRING)
  @Column(length = 20, name = "evaluation")
  @Comment("제품 평가 (BEST/GOOD/OKAY/BAD)")
  private FeedEvaluation evaluation;

  @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Builder.Default
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  @Comment("삭제 일시")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "feedThread", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @Comment("첨부 이미지 목록 (최대 5장)")
  private List<FeedThreadAttachmentFile> attachmentFiles = new ArrayList<>();
}
