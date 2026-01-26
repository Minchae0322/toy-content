package com.example.toycontent.app.common.exception.impl;



import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_EXIST(HttpStatus.BAD_REQUEST, "존재하지 않는 사용자입니다"),
    USER_NOT_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자입니다"),
    USER_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "사용자 서비스 오류가 발생했습니다"),
    PASSWORD_NOT_MATCHES(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "로그인에 실패했습니다. 사용자 정보를 확인해주세요"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다"),
    INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "올바른 인증 정보가 아닙니다"),
    USER_ID_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증 정보에서 사용자를 찾을 수 없습니다"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
