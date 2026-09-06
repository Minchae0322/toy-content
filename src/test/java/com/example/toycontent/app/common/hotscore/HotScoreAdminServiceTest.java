package com.example.toycontent.app.common.hotscore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.toycontent.app.battle.service.BattleHotScoreService;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.hotscore.HotScoreAdminService.RecalculateResult;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HotScoreAdminServiceTest {

  private HotScoreConfigService config;
  private FeedHotScoreService feed;
  private BattleHotScoreService battle;
  private ProductPopularityService product;
  private HotScoreAdminService service;

  @BeforeEach
  void setUp() {
    config = mock(HotScoreConfigService.class);
    feed = mock(FeedHotScoreService.class);
    battle = mock(BattleHotScoreService.class);
    product = mock(ProductPopularityService.class);
    service = new HotScoreAdminService(config, feed, battle, product);
  }

  @Test
  void 재계산은_그_도메인_하나만_돌린다() {
    when(battle.recalculateAll()).thenReturn(20);

    RecalculateResult r = service.recalculate(HotScoreDomain.BATTLE);

    verify(battle).recalculateAll();
    verify(feed, never()).recalculateAll();
    verify(product, never()).recalculateAll();
    assertThat(r.domain()).isEqualTo("battle");
    assertThat(r.recalculated()).isEqualTo(20);
  }

  @Test
  void 상수_변경은_저장_뒤_전체_재계산까지_한다() {
    when(feed.recalculateAll()).thenReturn(1234);

    RecalculateResult r = service.changeDivisor(HotScoreDomain.FEED, 2_592_000L);

    verify(config).update(HotScoreDomain.FEED, 2_592_000L);
    verify(feed).recalculateAll();
    assertThat(r.recalculated()).isEqualTo(1234);
  }

  @Test
  void 상수가_범위를_벗어나면_재계산하지_않는다() {
    when(config.update(any(), anyLong())).thenThrow(new IllegalArgumentException("range"));

    assertThatThrownBy(() -> service.changeDivisor(HotScoreDomain.PRODUCT, 10L))
        .isInstanceOf(RestApiException.class);
    verify(product, never()).recalculateAll();
  }

  @Test
  void 상수가_없으면_거부한다() {
    assertThatThrownBy(() -> service.changeDivisor(HotScoreDomain.FEED, null))
        .isInstanceOf(RestApiException.class);
    verify(config, never()).update(any(), anyLong());
  }
}
