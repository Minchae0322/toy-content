package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ContentType {

  BATTLE("BATTLE", "배틀"),
  FEED("FEED", "피드"),
  PRODUCT("PRODUCT", "핫아이템"),
  ;

  private String title;
  private String description;

  public static ContentType ofCode(String inputCode) {
    return Arrays.stream(values())
        .filter(v -> v.title.equals(inputCode))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("ContentType의 code 값이 올바르지 않습니다. 입력 값 : %s", inputCode)));
  }
}
