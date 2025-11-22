package com.example.toycontent.app.feed.controller.dto;

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

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class CommentUpdate {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
  }
}

