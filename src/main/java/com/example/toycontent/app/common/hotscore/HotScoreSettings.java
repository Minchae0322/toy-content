package com.example.toycontent.app.common.hotscore;

/**
 * 엔티티 메서드(Feed·Battle)가 스프링 빈 없이 시간 상수를 읽는 통로.
 * {@link HotScoreProperties}가 기동 시 한 번 채우고, 컨텍스트 없는 단위 테스트에서는 기본값을 쓴다.
 */
public final class HotScoreSettings {

  private static volatile long feedDivisor = HotScoreProperties.DEFAULT_FEED;
  private static volatile long battleDivisor = HotScoreProperties.DEFAULT_BATTLE;
  private static volatile long productDivisor = HotScoreProperties.DEFAULT_PRODUCT;

  private HotScoreSettings() {
  }

  static void apply(HotScoreProperties p) {
    feedDivisor = p.getFeedTimeDivisorSeconds();
    battleDivisor = p.getBattleTimeDivisorSeconds();
    productDivisor = p.getProductTimeDivisorSeconds();
  }

  public static long feedDivisor() {
    return feedDivisor;
  }

  public static long battleDivisor() {
    return battleDivisor;
  }

  public static long productDivisor() {
    return productDivisor;
  }
}
