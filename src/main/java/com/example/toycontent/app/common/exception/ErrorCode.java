package com.example.toycontent.app.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String name();
    HttpStatus getHttpStatus();
    String getMessage();

}
