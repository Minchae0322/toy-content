package com.example.toycontent.app.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.scheduler.controller.SchedulerAdminController;
import com.example.toycontent.app.scheduler.controller.SchedulerAdminController.RunResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchedulerAdminControllerTest {

  private FeedHotScoreScheduler feedHotScore;
  private BattleHotScoreScheduler battleHotScore;
  private SchedulerAdminController controller;

  @BeforeEach
  void setUp() {
    feedHotScore = mock(FeedHotScoreScheduler.class);
    battleHotScore = mock(BattleHotScoreScheduler.class);
    controller = new SchedulerAdminController(
        feedHotScore, battleHotScore,
        mock(ProductPopularityScheduler.class),
        mock(FeedTrendingScheduler.class),
        mock(BattleDeadlineNotificationScheduler.class));
  }

  @Test
  void 관리자는_작업을_실행하고_소요시간을_받는다() {
    RunResult result = controller.run("feed-hot-score.full", true).getBody().getData();

    verify(feedHotScore).fullRecalculate();
    verify(feedHotScore, never()).timeWeightUpdate();
    assertThat(result.job()).isEqualTo("feed-hot-score.full");
    assertThat(result.elapsedMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void 관리자가_아니면_실행되지_않는다() {
    assertThatThrownBy(() -> controller.run("feed-hot-score.time-weight", false))
        .isInstanceOf(RestApiException.class);
    verify(feedHotScore, never()).timeWeightUpdate();
  }

  @Test
  void 없는_작업이면_404계열_예외다() {
    assertThatThrownBy(() -> controller.run("nope", true))
        .isInstanceOf(RestApiException.class);
  }

  @Test
  void 목록에는_아홉_개_작업이_있다() {
    assertThat(controller.list(true).getBody().getData())
        .hasSize(9)
        .contains("feed-hot-score.time-weight", "feed-hot-score.full", "feed-trending");
  }
}
