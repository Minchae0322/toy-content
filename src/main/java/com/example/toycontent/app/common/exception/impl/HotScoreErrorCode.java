package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HotScoreErrorCode implements ErrorCode {
  DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "핫 스코어 도메인은 feed · battle · product 중 하나여야 합니다."),
  INVALID_TIME_DIVISOR(HttpStatus.BAD_REQUEST, "시간 상수는 1시간(3600초) 이상 1년 이하여야 합니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
