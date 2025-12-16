package com.example.toycontent.app.feed.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class FeedCommentRequest {

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class CommentCreate {

    @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class CommentUpdate {

    @Schema(description = "수정할 댓글 내용", example = "수정된 댓글입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
  }
}

