package com.example.toycontent.app.common.exception.impl;



import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_EXIST(HttpStatus.BAD_REQUEST, "user not exist"),
    PASSWORD_NOT_MATCHES(HttpStatus.BAD_REQUEST, "password not matches"),
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "login failed, user info invalid"),
    UNAUTHORIZED(HttpStatus.BAD_REQUEST, "user have not permission"),
    ;

    private final HttpStatus httpStatus;
    private final String message;


}
