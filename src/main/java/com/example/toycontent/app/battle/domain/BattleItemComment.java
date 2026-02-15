package com.example.toycontent.app.battle.domain;


import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "TB_BATTLE_ITEM_COMMENT")
public class BattleItemComment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_item_comment_id")
  @Comment("한줄 변론 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_item_id", nullable = false)
  @Comment("변론 대상 배틀 아이템")
  private BattleItem battleItem;

  @Column(name = "creator_id", nullable = false)
  @Comment("작성자")
  private Long creatorId;

  @Column(name = "creator_nickname", nullable = false, length = 30)
  @Comment("작성자 닉네임 (작성 시점 스냅샷)")
  private String creatorNickname;

  @Column(nullable = false, length = 100)
  @Comment("변론 내용 (최대 40자, 여유분 포함)")
  private String content;

  @Builder.Default
  @Column(nullable = false)
  @Comment("공감 수")
  private Integer likeCount = 0;

  @Builder.Default
  @Column(nullable = false)
  @Comment("삭제 여부")
  private Boolean isDeleted = false;

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "battleItemComment")
  private List<BattleItemCommentLike> likes = new ArrayList<>();

  // ========== 공감 ==========

  public void incrementLikeCount() {
    this.likeCount++;
  }

  public void decrementLikeCount() {
    this.likeCount = Math.max(0, this.likeCount - 1);
  }

  // ========== 수정 / 삭제 ==========

  public void updateContent(String content) {
    this.content = content;
  }

  public void softDelete() {
    this.isDeleted = true;
  }

  // ========== 조회 헬퍼 ==========

  public boolean isActive() {
    return !isDeleted;
  }

  public boolean isWrittenBy(Long memberId) {
    return this.creatorId.equals(memberId);
  }
}
