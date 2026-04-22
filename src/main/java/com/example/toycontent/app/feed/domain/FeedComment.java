package com.example.toycontent.app.feed.domain;


import com.example.toycontent.app.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_feed_comment")
public class FeedComment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id")
  private Feed feed;

  @NotNull
  @Column(name = "creator_id", nullable = false)
  @Comment("사용자 ID")
  private Long creatorId;

  @Column(name = "creator_nickname")
  private String creatorNickname;

  @Column(name = "creator_profile_url")
  private String creatorProfileUrl;

  @Column(name = "content", nullable = false, length = 200)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id")
  @Comment("부모 댓글 (답글인 경우). 1뎁스까지만 허용")
  private FeedComment parent;

  @NotNull
  @Builder.Default
  @Column(name = "deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  @Comment("삭제 여부")
  private Boolean deleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateContent(String content) {
    this.content = content;
  }

  public void delete() {
    this.deleted = true;
    deletedAt = LocalDateTime.now();
  }

  public boolean isReply() {
    return parent != null;
  }

}
