package com.example.toycontent.app.battle.controller.dto;

public interface Rankable extends Comparable<Rankable> {

  Long getId();

  Integer getTotalScore();

  void setRank(Integer rank);

  @Override
  default int compareTo(Rankable other) {
    return other.getTotalScore().compareTo(this.getTotalScore()); // 내림차순
  }

}