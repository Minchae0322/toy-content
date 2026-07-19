package com.example.toycontent.app.config;

import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code @Observed} AOP 활성화.
 *
 * <p>Spring Boot 3의 Observation API는 어노테이션 처리를 자동으로 하지 않아
 * {@link ObservedAspect}를 명시적으로 bean 등록해야 서비스/스케줄러 메서드에 붙인
 * {@code @Observed}가 span/메트릭으로 전환된다.
 */
@Configuration
public class TracingConfig {

  @Bean
  public ObservedAspect observedAspect(ObservationRegistry registry) {
    return new ObservedAspect(registry);
  }

  /**
   * Redis(Lettuce) 커맨드마다 span 생성 — 캐시 조회·ShedLock 경로. Spring Boot 가
   * 자동 구성하지 않아 수동 등록.
   */
  @Bean
  public ClientResourcesBuilderCustomizer lettuceObservationCustomizer(
      ObservationRegistry registry) {
    return builder -> builder.tracing(new MicrometerTracing(registry, "redis"));
  }
}
