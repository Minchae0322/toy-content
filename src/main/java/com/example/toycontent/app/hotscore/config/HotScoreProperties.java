package com.example.toycontent.app.hotscore.config;

import com.example.toycontent.app.hotscore.domain.HotScoreSettings;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 도메인별 시간 상수(초). "참여도 10배 = 이 시간" 이라는 뜻이다.
 *
 * <p>값을 바꾼 뒤에는 저장된 점수의 기준이 달라지므로 관리자 API로 전체 재계산을 한 번 돌린다.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hot-score")
public class HotScoreProperties {

  public static final long DEFAULT_FEED = 14L * 24 * 3600;      // 14일
  public static final long DEFAULT_BATTLE = 30L * 24 * 3600;    // 30일
  public static final long DEFAULT_PRODUCT = 30L * 24 * 3600;   // 30일

  private long feedTimeDivisorSeconds = DEFAULT_FEED;
  private long battleTimeDivisorSeconds = DEFAULT_BATTLE;
  private long productTimeDivisorSeconds = DEFAULT_PRODUCT;

  @PostConstruct
  void publish() {
    HotScoreSettings.apply(this);
  }
}
