package com.example.toycontent.app.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.toycontent.app.battle.service.BattleHotScoreService;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.feed.service.FeedHotScoreService;
import com.example.toycontent.app.product.service.ProductPopularityService;
import com.example.toycontent.app.scheduler.controller.SchedulerAdminController;
import com.example.toycontent.app.scheduler.controller.SchedulerAdminController.RunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerAdminControllerTest {

  private FeedHotScoreService feedHotScore;
  private BattleHotScoreService battleHotScore;
  private SchedulerAdminController controller;

  @BeforeEach
  void setUp() {
    feedHotScore = mock(FeedHotScoreService.class);
    battleHotScore = mock(BattleHotScoreService.class);
    controller = new SchedulerAdminController(
        feedHotScore, battleHotScore,
        mock(ProductPopularityService.class));
  }

  @Test
  void 관리자는_작업을_실행하고_소요시간을_받는다() {
    RunResult result = controller.run("feed-hot-score.recalculate", true).getBody().getData();

    verify(feedHotScore).recalculateAll();
    verify(battleHotScore, never()).recalculateAll();
    assertThat(result.job()).isEqualTo("feed-hot-score.recalculate");
    assertThat(result.elapsedMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void 관리자가_아니면_실행되지_않는다() {
    assertThatThrownBy(() -> controller.run("feed-hot-score.recalculate", false))
        .isInstanceOf(RestApiException.class);
    verify(feedHotScore, never()).recalculateAll();
  }

  @Test
  void 없는_작업이면_예외다() {
    assertThatThrownBy(() -> controller.run("nope", true))
        .isInstanceOf(RestApiException.class);
  }

  @Test
  void 목록에는_재계산_세_개만_있다() {
    assertThat(controller.list(true).getBody().getData())
        .containsExactly("feed-hot-score.recalculate", "battle-hot-score.recalculate",
            "product-popularity.recalculate");
  }
}
