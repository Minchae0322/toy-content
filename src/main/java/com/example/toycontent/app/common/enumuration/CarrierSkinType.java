package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CarrierSkinType {
    DEFAULT("DEFAULT", "기본"),
    // 필요한 스킨 타입 추가
    ;

    private final String code;
    private final String description;
}