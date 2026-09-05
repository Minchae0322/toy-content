package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SchedulerErrorCode implements ErrorCode {
  ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 스케줄러를 수동 실행할 수 있습니다."),
  JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스케줄러 작업입니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
