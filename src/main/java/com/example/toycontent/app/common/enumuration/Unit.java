package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Unit {

    CUSTOM("커스텀", "사용자 설정"),

    // 개수 단위
    EA_1("1개", "1개의 단일 물건"),
    EA_2("2개", "2개의 물건"),
    EA_3("3개", "3개의 물건"),
    EA_4("4개", "4개의 물건"),
    EA_5("5개", "5개의 물건"),
    EA_10("10개", "10개의 물건"),

    // 분수 단위
    QUARTER("1/4개", "4분의 1 단위"),
    HALF("1/2개", "절반 단위"),
    THREE_QUARTER("3/4개", "4분의 3 단위"),

    // 묶음/세트 단위
    ONE_SET("한 세트", "1개의 세트"),
    BUNDLE("묶음", "여러 개가 묶인 단위"),
    DOZEN("1다스", "12개 단위"),
    PACK("1팩", "작은 포장 단위"),
    BOX("1박스", "상자 단위"),
    BAG("1봉지", "비닐/포장 봉지 단위"),

    // 무게 단위
    GRAM_100("100g", "100그램"),
    GRAM_500("500g", "500그램"),
    KG_1("1kg", "1킬로그램"),
    KG_5("5kg", "5킬로그램"),

    // 부피 단위
    ML_500("500ml", "500밀리리터"),
    L_1("1L", "1리터"),
    L_2("2L", "2리터"),

    // 길이/장수
    METER("1미터", "길이 1m"),
    SHEET("1장", "종이, 쿠폰 등 낱장"),
    PAIR("1쌍", "2개 한 쌍(예: 신발)"),

    // 시간
    HOUR("1시간", "시간 단위"),
    DAY("1일", "하루 단위"),
    MONTH("1개월", "한 달 단위");


    private String title;
    private String description;

    public static Unit ofCode(String inputCode) {
        return Arrays.stream(values())
                .filter(v -> v.title.equals(inputCode))
                .findAny()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format("카테고리 코드가 올바르지 않습니다. 입력 값: %s", inputCode)));
    }
}
