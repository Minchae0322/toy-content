package com.example.toycontent.app.hotscore.controller.dto;

public class HotScoreRequest {

  /** @param timeDivisorSeconds "참여도 10배 = 이 시간(초)". 1시간 ~ 1년 */
  public record ChangeDivisor(Long timeDivisorSeconds) {
  }
}
