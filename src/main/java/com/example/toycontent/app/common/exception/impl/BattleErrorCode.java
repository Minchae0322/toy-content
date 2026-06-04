package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BattleErrorCode implements ErrorCode {

  // 배틀 기본
  BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "배틀을 찾을 수 없습니다."),
  BATTLE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 배틀입니다."),

  // 배틀 생성 권한
  BATTLE_CREATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "배틀을 생성할 수 없습니다."),
  INSUFFICIENT_LEVEL(HttpStatus.FORBIDDEN, "레벨이 부족합니다."),
  MAX_ACTIVE_BATTLES(HttpStatus.BAD_REQUEST, "동시 진행 가능한 배틀 수를 초과했습니다."),
  DAILY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "일일 배틀 생성 횟수를 초과했습니다."),

  // 배틀 생성 검증
  INVALID_BATTLE_PERIOD(HttpStatus.BAD_REQUEST, "배틀 기간이 유효하지 않습니다."),
  INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일과 종료일은 함께 입력해야 합니다."),
  INSUFFICIENT_BATTLE_ITEMS(HttpStatus.BAD_REQUEST, "아이템이 부족합니다."),
  TOO_MANY_BATTLE_ITEMS(HttpStatus.BAD_REQUEST, "아이템이 너무 많습니다."),
  INVALID_BATTLE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 배틀 타입입니다."),

  // 배틀 수정 검증
  ALREADY_START_BATTLE(HttpStatus.BAD_REQUEST, "이미 시작된 배틀은 수정할 수 없습니다."),

  // 배틀 아이템
  BATTLE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "배틀 아이템을 찾을 수 없습니다."),
  INVALID_BATTLE_ITEM(HttpStatus.BAD_REQUEST, "유효하지 않은 배틀 아이템입니다."),
  TOO_MANY_ITEMS(HttpStatus.BAD_REQUEST, "한 번에 추가할 수 있는 아이템 수를 초과했습니다."),
  EVENT_ID_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "해당 배틀은 이벤트 ID를 받지 않습니다."),
  INVALID_ITEM_STATUS(HttpStatus.BAD_REQUEST, "아이템 상태가 유효하지 않습니다."),
  CANNOT_VOTE_ITEM(HttpStatus.BAD_REQUEST, "투표할 수 없는 아이템입니다."),
  DUPLICATE_PRODUCT(HttpStatus.BAD_REQUEST, "이미 등록된 제품입니다."),

  // 배틀 상태
  BATTLE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "진행 중인 배틀이 아닙니다."),
  BATTLE_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "이미 종료된 배틀입니다."),
  BATTLE_NOT_STARTED(HttpStatus.BAD_REQUEST, "아직 시작되지 않은 배틀입니다."),
  INVALID_BATTLE_STATUS(HttpStatus.BAD_REQUEST, "배틀 상태가 유효하지 않습니다."),

  // 배틀 투표
  VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "투표 내역을 찾을 수 없습니다."),
  INVALID_VOTE_PERIOD(HttpStatus.BAD_REQUEST, "투표 기간이 아닙니다."),
  VOTE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "투표 가능 횟수를 초과했습니다."),
  INVALID_VOTE_COUNT(HttpStatus.BAD_REQUEST, "투표 수가 유효하지 않습니다."),
  INVALID_RANK(HttpStatus.BAD_REQUEST, "순위가 유효하지 않습니다."),
  DUPLICATE_RANK(HttpStatus.BAD_REQUEST, "중복된 순위로 투표할 수 없습니다."),
  INVALID_RANK_SEQUENCE(HttpStatus.BAD_REQUEST, "순위는 1위부터 순서대로 선택해야 합니다."),
  INVALID_VOTE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 투표 타입입니다."),
  INVALID_VOTE_RANK(HttpStatus.BAD_REQUEST, "투표 순위가 유효하지 않습니다."),
  DUPLICATE_VOTE_RANK(HttpStatus.BAD_REQUEST, "중복된 순위로 투표할 수 없습니다."),
  VOTE_NOT_ALLOWED_FOR_SWIPE(HttpStatus.BAD_REQUEST, "스와이프 배틀은 일반 투표가 불가합니다."),
  SWIPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "스와이프 배틀이 아닙니다."),

  // 배틀 권한
  FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
  NOT_BATTLE_CREATOR(HttpStatus.FORBIDDEN, "배틀 생성자만 수정할 수 있습니다."),
  CANNOT_PARTICIPATE(HttpStatus.FORBIDDEN, "참여할 수 없는 배틀입니다."),

  // 배틀 신고
  ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "이미 신고한 아이템입니다."),
  REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다."),
  CANNOT_REPORT_OWN_ITEM(HttpStatus.BAD_REQUEST, "본인이 등록한 아이템은 신고할 수 없습니다."),
  BATTLE_REPORT_DUPLICATED(HttpStatus.CONFLICT, "이미 신고한 배틀입니다."),
  BATTLE_REPORT_SELF(HttpStatus.BAD_REQUEST, "본인의 배틀은 신고할 수 없습니다."),

  // 배틀 참여
  PARTICIPATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "참여할 수 없습니다."),
  PARTICIPATION_PERIOD_NOT_STARTED(HttpStatus.BAD_REQUEST, "아직 참여 기간이 아닙니다."),
  MAX_PARTICIPANTS_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 참여자 수를 초과했습니다."),
  ALREADY_PARTICIPATED(HttpStatus.BAD_REQUEST, "이미 참여한 배틀입니다."),

  // 배틀 코멘트
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "코멘트를 찾을 수 없습니다."),
  COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 코멘트입니다."),
  NOT_COMMENT_WRITER(HttpStatus.FORBIDDEN, "본인이 작성한 코멘트만 수정/삭제할 수 있습니다."),
  COMMENT_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "코멘트 내용을 입력해주세요."),
  ALREADY_LIKED_COMMENT(HttpStatus.BAD_REQUEST, "이미 공감한 코멘트입니다."),
  ;

  private final HttpStatus httpStatus;
  private final String message;
}