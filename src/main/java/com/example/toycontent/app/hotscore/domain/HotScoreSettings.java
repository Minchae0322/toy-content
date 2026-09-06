package com.example.toycontent.app.hotscore.domain;

import com.example.toycontent.app.hotscore.config.HotScoreProperties;

/**
 * 엔티티 메서드(Feed·Battle)와 계산기가 스프링 빈 없이 시간 상수를 읽는 통로.
 * {@link HotScoreProperties}가 기동 시 기본값을 채우고, {@link HotScoreConfigService}가 Redis 값으로 덮어쓴다.
 * 컨텍스트 없는 단위 테스트에서는 기본값을 쓴다.
 */
public final class HotScoreSettings {

  private static volatile long feedDivisor = HotScoreProperties.DEFAULT_FEED;
  private static volatile long battleDivisor = HotScoreProperties.DEFAULT_BATTLE;
  private static volatile long productDivisor = HotScoreProperties.DEFAULT_PRODUCT;

  /** 다른 파드가 바꾼 값을 30초 안에 따라가기 위한 재조회 훅. HotScoreConfigService가 등록한다. */
  private static volatile Runnable refresher;
  private static volatile long lastRefreshNanos;
  private static final long REFRESH_INTERVAL_NANOS = 30L * 1_000_000_000L;

  private HotScoreSettings() {
  }

  public static void registerRefresher(Runnable r) {
    refresher = r;
    lastRefreshNanos = System.nanoTime();
  }

  private static void maybeRefresh() {
    Runnable r = refresher;
    if (r == null) {
      return;
    }
    long now = System.nanoTime();
    if (now - lastRefreshNanos > REFRESH_INTERVAL_NANOS) {
      lastRefreshNanos = now;
      r.run();
    }
  }

  public static void apply(HotScoreProperties p) {
    feedDivisor = p.getFeedTimeDivisorSeconds();
    battleDivisor = p.getBattleTimeDivisorSeconds();
    productDivisor = p.getProductTimeDivisorSeconds();
  }

  public static void set(HotScoreDomain domain, long seconds) {
    switch (domain) {
      case FEED -> feedDivisor = seconds;
      case BATTLE -> battleDivisor = seconds;
      case PRODUCT -> productDivisor = seconds;
    }
  }

  public static long of(HotScoreDomain domain) {
    maybeRefresh();
    return switch (domain) {
      case FEED -> feedDivisor;
      case BATTLE -> battleDivisor;
      case PRODUCT -> productDivisor;
    };
  }

  public static long feedDivisor() {
    maybeRefresh();
    return feedDivisor;
  }

  public static long battleDivisor() {
    maybeRefresh();
    return battleDivisor;
  }

  public static long productDivisor() {
    maybeRefresh();
    return productDivisor;
  }
}
