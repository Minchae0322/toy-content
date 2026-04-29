package com.example.toycontent.app.feedback.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class FeedbackRequest {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "의견 등록 요청")
  public static class Create {

    @Schema(description = "제목", example = "버튼 클릭이 안돼요", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다")
    private String title;

    @Schema(description = "내용", example = "메인 화면 우측 상단 알림 버튼 클릭 시 반응이 없습니다.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2000)
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 2000, message = "내용은 최대 2000자까지 입력 가능합니다")
    private String content;
  }
}
