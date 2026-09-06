package com.example.toycontent.app.scheduler;

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
import com.example.toycontent.app.common.hotscore.HotScoreConfigService;
import com.example.toycontent.app.common.hotscore.HotScoreDomain;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import com.example.toycontent.app.scheduler.controller.HotScoreAdminController;
import com.example.toycontent.app.scheduler.controller.HotScoreAdminController.UpdateRequest;
import com.example.toycontent.app.scheduler.controller.HotScoreAdminController.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HotScoreAdminControllerTest {

  private HotScoreConfigService configService;
  private FeedHotScoreService feedHotScore;
  private BattleHotScoreService battleHotScore;
  private ProductPopularityService productPopularity;
  private HotScoreAdminController controller;

  @BeforeEach
  void setUp() {
    configService = mock(HotScoreConfigService.class);
    feedHotScore = mock(FeedHotScoreService.class);
    battleHotScore = mock(BattleHotScoreService.class);
    productPopularity = mock(ProductPopularityService.class);
    controller = new HotScoreAdminController(configService, feedHotScore, battleHotScore, productPopularity);
  }

  @Test
  void 상수를_저장하고_그_도메인만_전체_재계산한다() {
    when(configService.update(HotScoreDomain.FEED, 2_592_000L)).thenReturn(2_592_000L);
    when(feedHotScore.recalculateAll()).thenReturn(1234);

    UpdateResult r = controller.update("feed", new UpdateRequest(2_592_000L), true).getBody().getData();

    verify(configService).update(HotScoreDomain.FEED, 2_592_000L);
    verify(feedHotScore).recalculateAll();
    verify(battleHotScore, never()).recalculateAll();
    verify(productPopularity, never()).recalculateAll();
    assertThat(r.domain()).isEqualTo("feed");
    assertThat(r.days()).isEqualTo(30.0);
    assertThat(r.recalculated()).isEqualTo(1234);
  }

  @Test
  void 상수가_범위를_벗어나면_재계산하지_않는다() {
    when(configService.update(any(), anyLong())).thenThrow(new IllegalArgumentException("range"));

    assertThatThrownBy(() -> controller.update("battle", new UpdateRequest(10L), true))
        .isInstanceOf(RestApiException.class);
    verify(battleHotScore, never()).recalculateAll();
  }

  @Test
  void 모르는_도메인은_거부한다() {
    assertThatThrownBy(() -> controller.update("comment", new UpdateRequest(86_400L), true))
        .isInstanceOf(RestApiException.class);
    verify(configService, never()).update(any(), anyLong());
  }

  @Test
  void 관리자가_아니면_거부한다() {
    assertThatThrownBy(() -> controller.update("feed", new UpdateRequest(86_400L), false))
        .isInstanceOf(RestApiException.class);
    verify(configService, never()).update(any(), anyLong());
  }
}
