package com.example.toycontent.app.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class TagParsingUtil {

  private static final String DELIMITER = ";";

  /**
   * 세미콜론으로 구분된 문자열을 List<String>으로 파싱
   * @param str 파싱할 문자열 (예: "신제품;최신;유행;한정판")
   * @return List<String> 파싱된 리스트
   */
  public static List<String> parseToList(String str) {
    if (str == null || str.trim().isEmpty()) {
      return new ArrayList<>();
    }

    return Arrays.stream(str.split(DELIMITER))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  /**
   * List<String>을 세미콜론으로 구분된 문자열로 변환
   * @param list 변환할 리스트
   * @return String 세미콜론으로 구분된 문자열
   */
  public static String listToString(List<String> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }

    return list.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(DELIMITER));
  }
}
