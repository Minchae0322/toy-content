package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SchedulerErrorCode implements ErrorCode {
  ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 실행할 수 있습니다."),
  JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 재계산 작업입니다."),
  DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 핫 스코어 도메인입니다 (feed · battle · product)."),
  INVALID_TIME_DIVISOR(HttpStatus.BAD_REQUEST, "시간 상수는 1시간(3600초) 이상 1년 이하여야 합니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
