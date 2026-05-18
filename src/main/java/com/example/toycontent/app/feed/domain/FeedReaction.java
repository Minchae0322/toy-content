package com.example.toycontent.app.feed.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.common.enumuration.ReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "tb_feed_reaction",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_feed_user_reaction_type",
        columnNames = {"feed_id", "user_id", "reaction_type"}
    ),
    indexes = {
        @Index(name = "idx_feed_reaction", columnList = "feed_id, reactionType")
    })
public class FeedReaction extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id", nullable = false)
  private Feed feed;

  @Column(nullable = false)
  @Comment("사용자 ID")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FeedReactionType reactionType;

  @Column(name = "is_active", nullable = false)
  @Comment("리액션 활성 여부 — 취소 시 false. 재리액션 시 true로 복귀하여 푸시 중복 방지에 사용.")
  @Builder.Default
  private boolean isActive = true;

  public static FeedReaction create(Feed feed, Long userId, FeedReactionType reactionType) {
    return FeedReaction.builder()
        .feed(feed)
        .userId(userId)
        .reactionType(reactionType)
        .isActive(true)
        .build();
  }

  public void deactivate() {
    this.isActive = false;
  }

  public void reactivate() {
    this.isActive = true;
  }

}
