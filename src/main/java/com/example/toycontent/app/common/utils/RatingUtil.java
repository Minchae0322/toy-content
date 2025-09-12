package com.example.toycontent.app.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * 평점 계산 유틸리티 클래스
 */
@UtilityClass
public class RatingUtil {

  /**
   * 평점 리스트로부터 평균 계산 (소수점 첫째자리까지)
   *
   * @param ratings 평점 리스트
   * @return 평균 평점
   */
  public static Double calculateAverage(List<Integer> ratings) {
    if (ratings == null || ratings.isEmpty()) {
      return 0.0;
    }

    double average = ratings.stream()
        .filter(rating -> rating != null && rating >= 1 && rating <= 5)
        .mapToInt(Integer::intValue)
        .average()
        .orElse(0.0);

    return BigDecimal.valueOf(average)
        .setScale(1, RoundingMode.HALF_UP)
        .doubleValue();
  }

  /**
   * 평점들을 받아서 평균 계산 (가변인자)
   *
   * @param ratings 평점들
   * @return 평균 평점
   */
  public static Double calculateAverage(Integer... ratings) {
    if (ratings == null || ratings.length == 0) {
      return 0.0;
    }

    return calculateAverage(List.of(ratings));
  }
}
