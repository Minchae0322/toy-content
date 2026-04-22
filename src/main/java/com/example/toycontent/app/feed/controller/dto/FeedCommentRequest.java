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

    @Schema(description = "부모 댓글 ID. 지정 시 해당 댓글에 대한 답글로 생성됨 (1뎁스까지만 허용)", example = "150")
    private Long parentCommentId;
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

