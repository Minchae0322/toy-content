package com.example.toycontent.app.reward.exp.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 응답용 EXP 지급 요약")
public record ExpGrantInfo(
    @Schema(description = "실제 지급된 EXP 총합 (0이면 미지급)") long amount,
    @Schema(description = "일일 캡에 걸린 지급이 포함됐는지") boolean capped
) {

  public static ExpGrantInfo aggregate(ExpGrantResult... results) {
    long total = 0;
    boolean capped = false;
    for (ExpGrantResult r : results) {
      if (r == null) continue;
      total += r.actualAmount();
      if (r.capped()) capped = true;
    }
    return new ExpGrantInfo(total, capped);
  }
}
