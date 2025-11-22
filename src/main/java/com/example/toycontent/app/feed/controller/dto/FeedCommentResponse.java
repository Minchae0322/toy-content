package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.feed.domain.FeedComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

public abstract class FeedCommentResponse {

  @Data
  @Builder
  @Schema(name = "FeedCommentCreatedResponse", description = "댓글 생성 응답 DTO")
  public static class Created {

    @Schema(description = "댓글 ID", example = "101")
    private Long commentId;

    @Schema(description = "피드 ID", example = "5")
    private Long feedId;

    @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!")
    private String content;

    @Schema(description = "작성자 ID", example = "42")
    private Long creatorID;

    public static Created of(FeedComment comment) {
      return Created.builder()
          .commentId(comment.getId())
          .feedId(comment.getFeed().getId())
          .content(comment.getContent())
          .creatorID(comment.getCreatorId())
          .build();
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