package com.example.toycontent.app.feed.domain;

import com.example.toycontent.app.oneMouth.domain.SalePost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "tb_feed_attachment_file", indexes = {
    // /feeds/scroll 응답 시 IN(feedIds) + is_primary 필터로 대표 썸네일 일괄 조회
    @Index(name = "idx_feed_attachment_feed_primary", columnList = "feed_id, is_primary")
})
public class FeedAttachmentFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id")
  private Feed feed;

  @Column(name = "attch_file_id")
  @Comment("첨부 파일 아이디")
  private Long attachFileId;

  @Column(name = "file_url", length = 2000)
  private String fileUrl;

  @Column(name = "org_file_nm")
  private String orgFileNm;

  @Column(name = "file_size")
  private Long fileSize; // 파일크기 표시용

  @Column(name = "content_type")
  private String contentType; // 파일타입 아이콘 표시용

  private Integer sortOrder;

  private String fileExplain;

  private Boolean isPrimary;
}

