package com.example.toycontent.app.hotscore.service;

import com.example.toycontent.app.hotscore.config.HotScoreProperties;
import com.example.toycontent.app.hotscore.domain.HotScoreDomain;
import com.example.toycontent.app.hotscore.domain.HotScoreSettings;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 핫 스코어 시간 상수의 런타임 저장소.
 *
 * <p>우선순위: Redis 저장값 → application.yml({@code hot-score.*-time-divisor-seconds}) 기본값.
 * 관리자 API로 바꾼 값은 Redis에 남아 재시작 후에도 유지되고, 기동 시 여기서 읽어
 * {@link HotScoreSettings}에 반영한다. 파드가 여럿이면 각 파드가 기동 때 Redis를 읽으므로
 * 값이 바뀐 뒤에는 전체 재계산과 함께 다른 파드의 반영도 필요하다 — 이 서비스의
 * {@link #update}는 Redis 갱신 + 현재 파드 반영까지 하고, 다른 파드는 {@link HotScoreSettings}가
 * 30초마다 한 번 {@link #refreshFromRedis()}를 불러 따라온다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotScoreConfigService {

  static final String KEY_PREFIX = "hot-score:time-divisor:";
  private static final long MIN_SECONDS = 3600;              // 1시간
  private static final long MAX_SECONDS = 365L * 24 * 3600;  // 1년

  private final StringRedisTemplate redis;
  private final HotScoreProperties defaults;

  @PostConstruct
  void loadOverrides() {
    refreshFromRedis();
    HotScoreSettings.registerRefresher(this::refreshFromRedis);
  }

  /** Redis에 저장된 값이 있으면 그것으로, 없으면 yml 기본값으로 현재 파드 설정을 맞춘다. */
  public void refreshFromRedis() {
    for (HotScoreDomain d : HotScoreDomain.values()) {
      Long stored = read(d);
      HotScoreSettings.set(d, stored != null ? stored : defaultOf(d));
    }
  }

  public Map<HotScoreDomain, Entry> current() {
    Map<HotScoreDomain, Entry> out = new EnumMap<>(HotScoreDomain.class);
    for (HotScoreDomain d : HotScoreDomain.values()) {
      Long stored = read(d);
      out.put(d, new Entry(stored != null ? stored : defaultOf(d), defaultOf(d), stored != null));
    }
    return out;
  }

  /** 상수를 바꾸고 현재 파드에 즉시 반영한다. 재계산은 호출자가 이어서 한다. */
  public long update(HotScoreDomain domain, long seconds) {
    if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
      throw new IllegalArgumentException(
          "timeDivisorSeconds는 " + MIN_SECONDS + " ~ " + MAX_SECONDS + " 사이여야 합니다");
    }
    redis.opsForValue().set(KEY_PREFIX + domain.key(), Long.toString(seconds));
    HotScoreSettings.set(domain, seconds);
    log.info("[hot-score] 시간 상수 변경 - {} = {}s ({}일)", domain.key(), seconds, seconds / 86400.0);
    return seconds;
  }

  private Long read(HotScoreDomain d) {
    try {
      String v = redis.opsForValue().get(KEY_PREFIX + d.key());
      return v != null ? Long.parseLong(v) : null;
    } catch (Exception e) {
      log.warn("[hot-score] Redis 상수 조회 실패, 기본값 사용 - {}: {}", d.key(), e.getMessage());
      return null;
    }
  }

  private long defaultOf(HotScoreDomain d) {
    return switch (d) {
      case FEED -> defaults.getFeedTimeDivisorSeconds();
      case BATTLE -> defaults.getBattleTimeDivisorSeconds();
      case PRODUCT -> defaults.getProductTimeDivisorSeconds();
    };
  }

  /**
   * @param timeDivisorSeconds 지금 적용 중인 값
   * @param defaultSeconds yml 기본값
   * @param overridden Redis에 저장된 값이 있어 기본값을 덮어쓰고 있는지
   */
  public record Entry(long timeDivisorSeconds, long defaultSeconds, boolean overridden) {
  }
}
