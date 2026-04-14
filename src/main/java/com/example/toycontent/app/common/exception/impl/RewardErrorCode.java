package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RewardErrorCode implements ErrorCode {

  // 뱃지
  BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "뱃지를 찾을 수 없습니다."),
  BADGE_CODE_DUPLICATED(HttpStatus.BAD_REQUEST, "이미 존재하는 뱃지 코드입니다."),

  // 유저 뱃지
  USER_BADGE_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 뱃지를 찾을 수 없습니다."),
  BADGE_ALREADY_ACQUIRED(HttpStatus.BAD_REQUEST, "이미 획득한 뱃지입니다."),
  BADGE_ALREADY_REVOKED(HttpStatus.BAD_REQUEST, "이미 회수된 뱃지입니다."),

  // 유저 보상 (EXP/레벨)
  USER_REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 보상 정보를 찾을 수 없습니다."),
  INVALID_EXP_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 경험치입니다."),

  // 스트릭
  USER_STREAK_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 스트릭 정보를 찾을 수 없습니다."),
  ALREADY_POSTED_TODAY(HttpStatus.BAD_REQUEST, "오늘 이미 인증 작성을 완료했습니다."),
  NO_RECOVERY_TICKET(HttpStatus.BAD_REQUEST, "복구 티켓이 없습니다."),

  // 일일 미션
  DAILY_MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "일일 미션을 찾을 수 없습니다."),
  DAILY_MISSION_CODE_DUPLICATED(HttpStatus.BAD_REQUEST, "이미 존재하는 미션 코드입니다."),

  // 유저 일일 미션 할당
  MISSION_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "미션 할당 정보를 찾을 수 없습니다."),
  MISSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 미션입니다."),
  MISSION_ALREADY_CLAIMED(HttpStatus.BAD_REQUEST, "이미 보상을 수령한 미션입니다."),
  MISSION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "미션이 아직 완료되지 않았습니다."),

  // 배틀 예측
  PREDICTION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 해당 배틀에 예측을 했습니다."),
  PREDICTION_ALREADY_SETTLED(HttpStatus.BAD_REQUEST, "이미 판정이 완료된 예측입니다."),

  // 카테고리 숙련도
  CATEGORY_MASTERY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리 마스터리 정보를 찾을 수 없습니다."),
  ;

  private final HttpStatus httpStatus;
  private final String message;
}
