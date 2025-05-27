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
public enum Category {

    ELECTRONICS("전자기기 및 가전제품", "전자기기 및 가전제품 관련 상품"),
    FURNITURE("가구", "가구 관련 상품"),
    HOME_GARDEN("홈 & 가든", "홈 & 가든 관련 상품"),
    BABY_KIDS("유아 & 아동용품", "유아 및 아동용품"),
    WOMENS_FASHION("여성 패션", "여성 의류 및 액세서리"),
    MENS_FASHION("남성 패션", "남성 의류 및 액세서리"),
    HEALTH_BEAUTY("건강 & 미용", "건강 및 미용 제품"),
    SPORTS_OUTDOORS("스포츠 & 아웃도어", "스포츠 및 아웃도어 용품"),
    GAMES_HOBBIES("게임 & 취미", "게임 및 취미 관련 상품"),
    BOOKS_MUSIC("도서 & 음악", "도서 및 음악 관련 상품"),
    PET_SUPPLIES("반려동물 용품", "반려동물 관련 상품"),
    ART_COLLECTIBLES("예술 & 수집품", "예술 작품 및 수집품"),
    VEHICLES_PARTS("차량 & 부품", "차량 및 부품"),
    OTHERS("기타", "기타 상품"),
    GARAGE_SALES("차고 세일", "차고 세일 관련 상품"),
    JOBS("구인구직", "구인구직 관련 정보");

    private String title;
    private String description;

    public static Category ofCode(String inputCode) {
        return Arrays.stream(values())
                .filter(v -> v.title.equals(inputCode))
                .findAny()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format("카테고리 코드가 올바르지 않습니다. 입력 값: %s", inputCode)));
    }
}
