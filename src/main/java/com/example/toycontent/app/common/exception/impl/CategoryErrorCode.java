package com.example.toycontent.app.common.exception.impl;


import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    CATEGORY_CODE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 카테고리 코드입니다."),
    INVALID_SORT_ORDER(HttpStatus.BAD_REQUEST, "정렬 순서는 1 이상이어야 합니다."),
    INVALID_TARGET_POSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 목표 위치입니다."),
    SAME_POSITION_MOVE(HttpStatus.BAD_REQUEST, "같은 위치로는 이동할 수 없습니다.");



    private final HttpStatus httpStatus;
    private final String message;


}
