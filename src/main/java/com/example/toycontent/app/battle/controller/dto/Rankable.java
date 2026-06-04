package com.example.toycontent.app.battle.controller.dto;

public interface Rankable extends Comparable<Rankable> {

  Long getId();

  Integer getTotalScore();

  void setRank(Integer rank);

  /**
   * 랭킹 정렬용 점수. 기본은 totalScore(vote 배틀), SWIPE 배틀처럼 통계 모델이 다른 경우 override.
   */
  default Integer getRankingScore() {
    return getTotalScore();
  }

  @Override
  default int compareTo(Rankable other) {
    return other.getRankingScore().compareTo(this.getRankingScore()); // 내림차순
  }

}