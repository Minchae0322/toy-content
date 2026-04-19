package com.example.toycontent.app.common.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExpSource {

  FEED_CREATE("피드 작성"),
  COMMENT_CREATE("댓글 작성"),
  BATTLE_VOTE("배틀 투표"),
  BATTLE_PREDICTION_HIT("배틀 예측 적중"),
  MISSION_CLAIM("일일 미션 보상 수령"),
  STREAK_BONUS("연속 출석 보너스"),
  ADMIN_GRANT("관리자 지급"),
  FEED_REACTION("피드 리액션 수신"),
  BATTLE_WEIGHTED_VOTE("가중 투표"),
  PICK_COMMENT("PICK 한마디"),
  HOT_DISCOVER("HOT 발굴자"),
  WEEKLY_RANKING("주간 랭킹"),
  BATTLE_RESULT("배틀 결과"),
  ATTENDANCE("출석"),
  ;

  private final String description;
}
