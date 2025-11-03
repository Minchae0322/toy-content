package com.example.toycontent.app.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 스케줄러 설정을 YAML에서 읽어오는 Properties 클래스
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scheduler.hot-score")
public class SchedulerProperties {

  /**
   * 전체 스케줄러 활성화 여부
   */
  private boolean enable;

  /**
   * 시간 가중치 업데이트 설정
   */
  private TimeWeightUpdate timeWeightUpdate = new TimeWeightUpdate();

  /**
   * 전체 재계산 설정
   */
  private FullRecalculate fullRecalculate = new FullRecalculate();

  @Getter
  @Setter
  public static class TimeWeightUpdate {
    private String cron;
    private boolean enabled;
  }

  @Getter
  @Setter
  public static class FullRecalculate {
    private String cron;
    private boolean enabled;
  }
}