package com.example.toycontent.app.hotscore.controller.dto;

public class HotScoreResponse {

  /** @param overridden Redis 저장값이 yml 기본값을 덮어쓰고 있는지 */
  public record DivisorStatus(String domain, long timeDivisorSeconds, long defaultSeconds, boolean overridden) {
    public double days() {
      return timeDivisorSeconds / 86400.0;
    }
  }

  public record RecalculateResult(String domain, long timeDivisorSeconds, int recalculated, long elapsedMs) {
    public double days() {
      return timeDivisorSeconds / 86400.0;
    }
  }
}
