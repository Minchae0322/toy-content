package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.common.enumuration.SwipeVerdict;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class BattleSwipeRequest {

  @Schema(description = "스와이프 1건 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Swipe {

    @Schema(description = "배틀 아이템 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long itemId;

    @Schema(description = "평가 결과", example = "STRONG_PICK",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private SwipeVerdict verdict;
  }
}
