package com.example.toycontent.app.common.enumuration;

import com.example.toycontent.app.battle.domain.BattleItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;

/**
 * 배틀의 평가 방식.
 *
 * <p>새 voteType 추가 시 abstract 메서드 구현이 강제되므로, 분기 누락이 컴파일 단계에서 막힌다.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum VoteType {
  SINGLE("SINGLE", "1인 1표") {
    @Override
    public Integer rankingScoreOf(BattleItem item) {
      return item.getTotalScore();
    }
  },
  MULTIPLE("MULTIPLE", "1인 3표") {
    @Override
    public Integer rankingScoreOf(BattleItem item) {
      return item.getTotalScore();
    }
  },
  SWIPE("SWIPE", "스와이프(강추/PICK/PASS)") {
    @Override
    public Integer rankingScoreOf(BattleItem item) {
      return item.getSwipeRankingScore();
    }
  },
  ;

  private final String title;
  private final String description;

  VoteType(String title, String description) {
    this.title = title;
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  /**
   * voteType별 랭킹 점수 계산식. 새 voteType은 자기 점수 모델을 여기에 정의.
   */
  public abstract Integer rankingScoreOf(BattleItem item);

  public static VoteType getVoteType(String title) {
    return Arrays.stream(values())
        .filter(type -> type.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid VoteType: " + title));
  }
}
