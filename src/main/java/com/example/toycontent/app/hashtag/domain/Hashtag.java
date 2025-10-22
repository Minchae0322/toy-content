package com.example.toycontent.app.hashtag.domain;

import com.example.toycontent.app.common.BaseTimeEntity;
import com.example.toycontent.app.feed.domain.FeedHashtag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_hashtags",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_hashtag_name", columnNames = "name")
    },
    indexes = {
        @Index(name = "idx_hashtag_name", columnList = "name"),
        @Index(name = "idx_hashtag_usage_count", columnList = "usage_count")
    }
)
@Comment("해시태그")
public class Hashtag extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Comment("해시태그 ID")
  private Long id;

  @Column(nullable = false, length = 50, unique = true)
  @Comment("해시태그 이름 (# 제외)")
  private String name;

  @Column(nullable = false)
  @Comment("사용 횟수")
  @Builder.Default
  private Long usageCount = 0L;

  @OneToMany(mappedBy = "hashtag", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @Comment("피드-해시태그 연결 목록")
  private List<FeedHashtag> feedHashtags = new ArrayList<>();

  /**
   * 사용 횟수 증가
   */
  public void incrementUsageCount() {
    this.usageCount++;
  }

  /**
   * 사용 횟수 감소
   */
  public void decrementUsageCount() {
    if (this.usageCount > 0) {
      this.usageCount--;
    }
  }

  /**
   * 해시태그 이름 정규화
   */
  public static String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    // # 제거, 공백 제거, 소문자 변환
    return name.trim()
        .replace("#", "")
        .toLowerCase();
  }
}
