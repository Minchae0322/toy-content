package com.example.toycontent.app.common.exception;

import lombok.Getter;

@Getter
public class RestApiException extends RuntimeException {

    private  ErrorCode errorCode;

    public RestApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

