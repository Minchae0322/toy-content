package com.example.toycontent.app.hotscore.domain;

/** 핫 스코어 시간 상수를 따로 갖는 도메인. 관리자 API 경로 변수로 쓴다 (feed · battle · product). */
public enum HotScoreDomain {
  FEED, BATTLE, PRODUCT;

  public String key() {
    return name().toLowerCase();
  }
}
