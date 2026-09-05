package com.example.toycontent.app.common.hotscore;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Reddit식 핫 스코어. 시간이 흘러도 값이 변하지 않아 배치 재계산이 필요 없다.
 *
 * <pre>
 *   score = log10(max(engagement, 1)) + epochSeconds(anchor) / timeDivisorSeconds
 * </pre>
 *
 * <ul>
 *   <li>참여도 10배 = 1점. 시간 항은 {@code timeDivisorSeconds}마다 1점.</li>
 *   <li>즉 "참여도 10배 = timeDivisor만큼의 시간" 이 교환 비율이다. 피드 14일, 배틀·제품 30일.</li>
 *   <li>점수는 참여도가 바뀌는 그 행에서만 갱신한다. 새 글은 시간 항이 커서 위에서 시작하고,
 *       옛 글은 참여도로 그 차이를 메워야 올라온다.</li>
 *   <li>상수를 바꾸면 저장된 점수의 기준이 달라지므로 전체 재계산을 한 번 돌려야 한다.</li>
 * </ul>
 */
public final class HotScoreFormula {

  private HotScoreFormula() {
  }

  public static double score(double engagement, LocalDateTime anchor, long timeDivisorSeconds) {
    double engagementTerm = Math.log10(Math.max(engagement, 1.0));
    long epochSeconds = (anchor != null ? anchor : LocalDateTime.now()).toEpochSecond(ZoneOffset.UTC);
    return engagementTerm + (double) epochSeconds / timeDivisorSeconds;
  }

  /** MySQL에서 같은 값을 만드는 식. {@code :divisor} 파라미터를 바인딩한다. */
  public static String sql(String engagementExpr, String anchorColumn) {
    return "LOG10(GREATEST(" + engagementExpr + ", 1)) + UNIX_TIMESTAMP(" + anchorColumn + ") / :divisor";
  }
}
