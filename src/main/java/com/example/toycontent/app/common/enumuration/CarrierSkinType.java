package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CarrierSkinType {

    DEFAULT("DEFAULT", "기본"),
    RETRO("RETRO", "레트로 트래블"),
    SPACE("SPACE", "스페이스"),
    KITTY("KITTY", "헬로키티"),
    ;

    private final String code;
    private final String description;
}
