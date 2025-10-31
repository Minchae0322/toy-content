package com.example.toycontent.app.battle.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class BattleVoteRequest {

  @Schema(description = "배틀 투표 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Vote {

    @Schema(description = "투표할 아이템 목록")
    @Valid
    @NotNull(message = "투표할 아이템을 선택해주세요")
    @Size(min = 1, max = 3, message = "투표는 1~3개까지 가능합니다")
    private List<VoteItem> votes;
  }

  @Schema(description = "투표 아이템")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VoteItem {

    @Schema(description = "아이템 ID", example = "1")
    @NotNull(message = "아이템을 선택해주세요")
    private Long itemId;

    @Schema(description = "순위 (1위=3점, 2위=2점, 3위=1점)", example = "1")
    @NotNull(message = "순위를 선택해주세요")
    @Min(value = 1, message = "순위는 1 이상이어야 합니다")
    @Max(value = 3, message = "순위는 3 이하여야 합니다")
    private Integer rank;
  }

}
