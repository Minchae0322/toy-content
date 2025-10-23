package com.example.toycontent.app.feed.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import jakarta.persistence.Entity;
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

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_feed_hashtags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_feed_hashtag", columnNames = {"feed_id", "hashtag_id"})
    },
    indexes = {
        @Index(name = "idx_feed_id", columnList = "feed_id"),
        @Index(name = "idx_hashtag_id", columnList = "hashtag_id")
    }
)
@Comment("피드-해시태그 연결")
public class FeedHashtag extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Comment("피드-해시태그 연결 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id", nullable = false)
  @Comment("피드")
  private Feed feed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hashtag_id", nullable = false)
  @Comment("해시태그")
  private Hashtag hashtag;

  /**
   * 연관관계 편의 메서드
   */
  public void setFeed(Feed feed) {
    this.feed = feed;
    if (feed != null && !feed.getHashtags().contains(this)) {
      feed.getHashtags().add(this);
    }
  }

  public void setHashtag(Hashtag hashtag) {
    this.hashtag = hashtag;
    if (hashtag != null && !hashtag.getFeedHashtags().contains(this)) {
      hashtag.getFeedHashtags().add(this);
    }
  }

  /**
   * 정적 팩토리 메서드
   */
  public static FeedHashtag create(Feed feed, Hashtag hashtag) {
    FeedHashtag feedHashtag = FeedHashtag.builder()
        .feed(feed)
        .hashtag(hashtag)
        .build();

    feedHashtag.setFeed(feed);
    feedHashtag.setHashtag(hashtag);

    return feedHashtag;
  }
}
