package com.example.toycontent.app.reward.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "EXP 지급 결과")
public record ExpGrantResult(
    @Schema(description = "지급 여부") boolean granted,
    @Schema(description = "요청 EXP") long requestedAmount,
    @Schema(description = "실제 지급 EXP (캡 적용 후)") long actualAmount,
    @Schema(description = "일일 캡에 걸렸는지") boolean capped,
    @Schema(description = "중복 지급 시도였는지") boolean duplicate
) {

  public static ExpGrantResult granted(long requested, long actual, boolean capped) {
    return new ExpGrantResult(true, requested, actual, capped, false);
  }

  public static ExpGrantResult duplicated(long requested) {
    return new ExpGrantResult(false, requested, 0, false, true);
  }

  public static ExpGrantResult cappedOut(long requested) {
    return new ExpGrantResult(false, requested, 0, true, false);
  }
}
