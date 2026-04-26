package com.example.toycontent.app.common.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExpSource {

  FEED_CREATE("피드 작성", 30L, false),
  COMMENT_CREATE("댓글 작성", 5L, false),
  BATTLE_VOTE("배틀 투표", 10L, false),
  BATTLE_CREATE("배틀 생성", 20L, false),
  BATTLE_ITEM_ADD("배틀 아이템 추가", 15L, false),
  BATTLE_PREDICTION_HIT("배틀 예측 적중", 100L, false),
  MISSION_CLAIM("일일 미션 보상 수령", 0L, true),
  STREAK_BONUS("연속 출석 보너스", 0L, true),
  ADMIN_GRANT("관리자 지급", 0L, true),
  FEED_REACTION("피드 리액션 수신", 5L, false),
  BATTLE_WEIGHTED_VOTE("가중 투표", 10L, false),
  PICK_COMMENT("PICK 한마디", 5L, false),
  HOT_DISCOVER("HOT 발굴자", 50L, false),
  WEEKLY_RANKING("주간 랭킹", 0L, true),
  BATTLE_RESULT("배틀 결과", 0L, true),
  ATTENDANCE("출석", 5L, false),
  ;

  private final String description;
  private final long defaultAmount;
  private final boolean capExempt;
}
