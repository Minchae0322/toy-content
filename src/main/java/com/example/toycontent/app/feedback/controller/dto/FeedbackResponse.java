package com.example.toycontent.app.feedback.controller.dto;

import com.example.toycontent.app.feedback.domain.Feedback;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class FeedbackResponse {

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "의견 목록 항목")
  public static class ListView {

    @Schema(description = "의견 ID")
    private Long id;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "내용")
    private String content;

    @Schema(description = "등록 일시")
    private LocalDateTime createdAt;

    public static ListView from(Feedback feedback) {
      return ListView.builder()
          .id(feedback.getId())
          .title(feedback.getTitle())
          .content(feedback.getContent())
          .createdAt(feedback.getCreatedAt())
          .build();
    }
  }
}
