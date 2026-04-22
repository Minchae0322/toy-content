package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.feed.domain.FeedComment;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class FeedCommentResponse {

  public static final String DELETED_CONTENT_MESSAGE = "삭제된 댓글입니다.";

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "CommentResponse", description = "댓글 응답 DTO (답글 포함)")
  public static class CommentItem {
    @Schema(description = "댓글 ID", example = "150")
    private Long commentId;

    @Schema(description = "작성자 ID", example = "42")
    private Long creatorId;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String creatorNickname;

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/profile/42.jpg")
    private String creatorProfileUrl;

    @Schema(description = "댓글 내용. 삭제된 댓글인 경우 '삭제된 댓글입니다.' 고정 문구 반환", example = "좋은 글이네요!")
    private String content;

    @Schema(description = "삭제 여부 (true면 부모 댓글이 삭제됐으나 답글이 남아있어 노출됨)", example = "false")
    private Boolean deleted;

    @Schema(description = "작성일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "답글 목록 (1뎁스). 삭제되지 않은 답글만 포함")
    private List<ReplyItem> replies;

    public static CommentItem active(FeedComment comment, List<ReplyItem> replies) {
      return CommentItem.builder()
          .commentId(comment.getId())
          .creatorId(comment.getCreatorId())
          .creatorNickname(comment.getCreatorNickname())
          .creatorProfileUrl(comment.getCreatorProfileUrl())
          .content(comment.getContent())
          .deleted(false)
          .createdAt(comment.getCreatedAt())
          .updatedAt(comment.getUpdatedAt())
          .replies(replies != null ? replies : new ArrayList<>())
          .build();
    }

    public static CommentItem deletedWithReplies(FeedComment comment, List<ReplyItem> replies) {
      return CommentItem.builder()
          .commentId(comment.getId())
          .creatorId(null)
          .creatorNickname(null)
          .creatorProfileUrl(null)
          .content(DELETED_CONTENT_MESSAGE)
          .deleted(true)
          .createdAt(comment.getCreatedAt())
          .updatedAt(comment.getUpdatedAt())
          .replies(replies)
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "ReplyResponse", description = "답글 응답 DTO")
  public static class ReplyItem {
    @Schema(description = "답글 ID", example = "151")
    private Long commentId;

    @Schema(description = "부모 댓글 ID", example = "150")
    private Long parentCommentId;

    @Schema(description = "작성자 ID", example = "42")
    private Long creatorId;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String creatorNickname;

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/profile/42.jpg")
    private String creatorProfileUrl;

    @Schema(description = "답글 내용", example = "저도 동의해요!")
    private String content;

    @Schema(description = "작성일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public static ReplyItem from(FeedComment reply) {
      return ReplyItem.builder()
          .commentId(reply.getId())
          .parentCommentId(reply.getParent() != null ? reply.getParent().getId() : null)
          .creatorId(reply.getCreatorId())
          .creatorNickname(reply.getCreatorNickname())
          .creatorProfileUrl(reply.getCreatorProfileUrl())
          .content(reply.getContent())
          .createdAt(reply.getCreatedAt())
          .updatedAt(reply.getUpdatedAt())
          .build();
    }
  }

  @Data
  @Builder
  @Schema(name = "FeedCommentCreatedResponse", description = "댓글 생성 응답 DTO")
  public static class Created {

    @Schema(description = "댓글 ID", example = "101")
    private Long commentId;

    @Schema(description = "피드 ID", example = "5")
    private Long feedId;

    @Schema(description = "부모 댓글 ID (답글인 경우에만 존재)", example = "100")
    private Long parentCommentId;

    @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!")
    private String content;

    @Schema(description = "작성자 ID", example = "42")
    private Long creatorID;

    @Schema(description = "지급된 EXP 정보 (없으면 null)")
    private ExpGrantInfo expGrant;

    public static Created of(FeedComment comment, ExpGrantInfo expGrant) {
      return Created.builder()
          .commentId(comment.getId())
          .feedId(comment.getFeed().getId())
          .parentCommentId(comment.getParent() != null ? comment.getParent().getId() : null)
          .content(comment.getContent())
          .creatorID(comment.getCreatorId())
          .expGrant(expGrant)
          .build();
    }

    public static Created of(FeedComment comment) {
      return of(comment, null);
    }

  }

  @Data
  @Builder
  @Schema(name = "FeedCommentUpdatedResponse", description = "댓글 수정 응답 DTO")
  public static class Updated {

    @Schema(description = "댓글 ID", example = "101")
    private Long commentId;

    @Schema(description = "피드 ID", example = "5")
    private Long feedId;

    @Schema(description = "댓글 내용", example = "수정된 댓글 내용입니다.")
    private String content;

    public static Updated of(FeedComment comment) {
      return Updated.builder()
          .commentId(comment.getId())
          .feedId(comment.getFeed().getId())
          .content(comment.getContent())
          .build();
    }
  }
}
