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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Builder
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "TB_BATTLE_ITEM_COMMENT_LIKE",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_ITEM_COMMENT_LIKE_MEMBER",
            columnNames = {"battle_item_comment_id", "creator_id"}
        )
    }
)
public class BattleItemCommentLike extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "battle_item_comment_like_id")
  @Comment("배틀 아이템 댓글 좋아요 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "battle_item_comment_id", nullable = false)
  @Comment("배틀 아이템 댓글")
  private BattleItemComment battleItemComment;

  @Column(name = "creator_id")
  @Comment("좋아요 유저 아이디")
  private Long creatorId;
}
