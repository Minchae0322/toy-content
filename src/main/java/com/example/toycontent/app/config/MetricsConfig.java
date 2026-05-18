package com.example.toycontent.app.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 메트릭 카디널리티 방어 설정.
 * URI 태그가 폭증하면 Grafana Cloud Free의 10k 시리즈 한도가 빠르게 소진되므로
 * (1) 카디널리티 상한, (2) actuator 자체 호출 제외, (3) 매칭 실패 URI 통합의 3중 방어를 둔다.
 */
@Configuration
public class MetricsConfig {

  private static final String METRIC_HTTP_SERVER_REQUESTS = "http.server.requests";

  private static final String TAG_URI = "uri";
  private static final String TAG_STATUS = "status";
  private static final String TAG_METHOD = "method";
  private static final String TAG_EXCEPTION = "exception";
  private static final String TAG_OUTCOME = "outcome";

  private static final int MAX_URI_TAG_VALUES = 100;
  private static final String ACTUATOR_PREFIX = "/actuator";
  private static final String UNMATCHED_URI = "UNMATCHED";
  private static final Set<String> UNMATCHED_STATUSES = Set.of("404", "405");

  /**
   * uri 태그 카디널리티 상한 — 한도 초과 시 새 URI는 메트릭에서 누락되지만 시리즈 폭증은 차단.
   * 정상 트래픽이 이 한도에 닿으면 그 자체가 다른 방어 계층의 결함을 알리는 신호.
   */
  @Bean
  public MeterFilter httpUriCardinalityLimit() {
    return MeterFilter.maximumAllowableTags(
        METRIC_HTTP_SERVER_REQUESTS, TAG_URI, MAX_URI_TAG_VALUES, MeterFilter.deny());
  }

  /**
   * Spring Boot 3 Observation API가 추가한 `error` 태그는 기존 `exception` 태그와 값이 동일.
   * 양쪽 다 유지하면 시리즈 차원이 한 단계 더 곱해져 Grafana Cloud Free 10k 한도를 압박한다.
   */
  @Bean
  public MeterFilter dropRedundantErrorTag() {
    return MeterFilter.ignoreTags("error");
  }

  /** Alloy/Prometheus scrape가 자기 자신을 측정하는 루프를 만들지 않도록 제외. */
  @Bean
  public MeterFilter denyActuatorUri() {
    return MeterFilter.deny(id -> {
      String uri = id.getTag(TAG_URI);
      return uri != null && uri.startsWith(ACTUATOR_PREFIX);
    });
  }

  /**
   * 컨트롤러에 매칭되지 않는 요청(봇 스캐너, 오타 등)의 uri 태그를 단일 값으로 통합.
   * 개별 경로는 로그/트레이스에서 확인.
   */
  @Bean
  public MeterFilter normalizeUnmatchedUri() {
    return new MeterFilter() {
      @Override
      public Meter.Id map(Meter.Id id) {
        if (!METRIC_HTTP_SERVER_REQUESTS.equals(id.getName())) return id;

        String status = id.getTag(TAG_STATUS);
        if (!UNMATCHED_STATUSES.contains(status)) return id;

        return id.replaceTags(List.of(
            Tag.of(TAG_URI, UNMATCHED_URI),
            Tag.of(TAG_STATUS, status),
            Tag.of(TAG_METHOD, orDefault(id, TAG_METHOD, "UNKNOWN")),
            Tag.of(TAG_EXCEPTION, orDefault(id, TAG_EXCEPTION, "None")),
            Tag.of(TAG_OUTCOME, orDefault(id, TAG_OUTCOME, "CLIENT_ERROR"))
        ));
      }
    };
  }

  private static String orDefault(Meter.Id id, String key, String fallback) {
    String value = id.getTag(key);
    return value != null ? value : fallback;
  }
}
