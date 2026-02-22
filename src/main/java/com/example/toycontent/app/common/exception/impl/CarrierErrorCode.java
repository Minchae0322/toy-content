package com.example.toycontent.app.common.exception.impl;

import com.example.toycontent.app.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CarrierErrorCode implements ErrorCode {

    // 캐리어 기본
    CARRIER_NOT_FOUND(HttpStatus.NOT_FOUND, "캐리어를 찾을 수 없습니다."),
    CARRIER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "존재하지 않거나 권한이 없는 캐리어입니다."),

    // 캐리어 생성/삭제
    MAX_CARRIER_EXCEEDED(HttpStatus.BAD_REQUEST, "캐리어 최대 생성 수를 초과했습니다."),
    DEFAULT_CARRIER_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "기본 캐리어는 삭제할 수 없습니다."),

    // 캐리어 아이템
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 아이템입니다."),
    ITEM_DUPLICATE_PRODUCT(HttpStatus.BAD_REQUEST, "이미 캐리어에 담긴 상품입니다."),
    MAX_ITEM_EXCEEDED(HttpStatus.BAD_REQUEST, "캐리어 최대 아이템 수를 초과했습니다."),
    ITEM_NOT_IN_CARRIER(HttpStatus.BAD_REQUEST, "존재하지 않는 아이템이 포함되어 있습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),

    // 캐리어 스티커
    STICKER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스티커입니다."),
    MAX_STICKER_EXCEEDED(HttpStatus.BAD_REQUEST, "캐리어 최대 스티커 수를 초과했습니다."),
    STICKER_NOT_IN_CARRIER(HttpStatus.BAD_REQUEST, "존재하지 않는 스티커가 포함되어 있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}