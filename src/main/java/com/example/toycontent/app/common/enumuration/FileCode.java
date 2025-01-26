package com.example.toycontent.app.common.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum FileCode {

    PRODUCT("PRODUCT", "제품 업로드 파일"),
    ;

    private String title;
    private String description;

    public static FileCode ofCode(String inputCode) {
        return Arrays.stream(values())
                .filter(v -> v.title.equals(inputCode))
                .findAny()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format("statusCode의 code 값이 올바르지 않습니다. 입력 값 : %s", inputCode)));
    }
}
