package com.example.toycontent.app.config;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 메트릭 카디널리티 방어 설정.
 * - URI 태그가 폭증하면 Grafana Cloud Free의 10k 시리즈 한도가 빠르게 소진됨.
 * - actuator 자체 호출 메트릭은 모니터링 노이즈라 제거.
 */
@Configuration
public class MetricsConfig {

  private static final int MAX_URI_TAG_VALUES = 100;

  /**
   * http.server.requests의 uri 태그 카디널리티 상한.
   * 한도 초과 시 deny — 새 URI는 메트릭에서 누락되지만 시리즈 폭증은 차단.
   */
  @Bean
  public MeterFilter httpUriCardinalityLimit() {
    return MeterFilter.maximumAllowableTags(
        "http.server.requests", "uri", MAX_URI_TAG_VALUES, MeterFilter.deny());
  }

  /**
   * /actuator/* 경로 요청 메트릭 제외 — Alloy/Prometheus scrape 자체가 노이즈를 만들기 때문.
   */
  @Bean
  public MeterFilter denyActuatorUri() {
    return MeterFilter.deny(id -> {
      String uri = id.getTag("uri");
      return uri != null && uri.startsWith("/actuator");
    });
  }
}
