package com.example.toycontent.app.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.event.FeedViewedEvent;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.reward.exp.service.UserRewardService;
import com.example.toycontent.support.fixture.FeedFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 조회 핫패스의 DB 동작 검증. 종전 FeedServiceTest.GetFeed에 있던 계약(조회수 불변 ·
 * 이벤트 발행 · 피드 없음 예외)이 트랜잭션 경계 분리(2026-08-30)로 이 로더로 이사했다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedQueryService")
class FeedQueryServiceTest {

  private static final long FEED_ID = 1L;
  private static final long FEED_OWNER_ID = 100L;

  @Mock private FeedRepository feedRepository;
  @Mock private FeedReactionRepository feedReactionRepository;
  @Mock private UserRewardService userRewardService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private FeedQueryService feedQueryService;

  @Nested
  @DisplayName("loadDetail - 단건 조회 로더")
  class LoadDetail {

    @Test
    @DisplayName("조회는 조회수를 바꾸지 않는다 - 증가는 FeedViewedEvent 리스너로 분리됐다")
    void 조회는_조회수를_바꾸지_않는다() {
      // given
      Feed feed = FeedFixture.withId(FEED_ID);
      int previousViewCount = feed.getViewCount();
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndIsActiveTrue(FEED_ID, FEED_OWNER_ID))
          .willReturn(List.of());

      // when
      feedQueryService.loadDetail(FEED_ID, FEED_OWNER_ID);

      // then
      assertThat(feed.getViewCount())
          .as("조회 이후 조회수 - loadDetail은 readOnly라 엔티티를 건드리지 않는다")
          .isEqualTo(previousViewCount);
    }

    @Test
    @DisplayName("조회가 끝나면 FeedViewedEvent를 발행한다 - UPDATE는 커밋 후 리스너 몫")
    void 조회는_이벤트를_발행한다() {
      // given
      Feed feed = FeedFixture.withId(FEED_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndIsActiveTrue(FEED_ID, FEED_OWNER_ID))
          .willReturn(List.of());

      // when
      feedQueryService.loadDetail(FEED_ID, FEED_OWNER_ID);

      // then
      then(eventPublisher).should().publishEvent(new FeedViewedEvent(FEED_ID));
      then(feedRepository).should(never()).incrementViewCount(eq(FEED_ID), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 피드 조회 시 RestApiException을 던진다")
    void 피드_없음_예외() {
      // given
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedQueryService.loadDetail(FEED_ID, FEED_OWNER_ID))
          .isInstanceOf(RestApiException.class);

      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("userInfo는 채우지 않는다 - Redis/HTTP는 트랜잭션 밖에서 호출자 몫")
    void userInfo는_null로_둔다() {
      // given
      Feed feed = FeedFixture.withId(FEED_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(feedReactionRepository.findByFeedIdAndUserIdAndIsActiveTrue(FEED_ID, FEED_OWNER_ID))
          .willReturn(List.of());

      // when
      FeedQueryService.DetailView result = feedQueryService.loadDetail(FEED_ID, FEED_OWNER_ID);

      // then
      assertThat(result.detail().getUserInfo()).isNull();
      assertThat(result.creatorId()).isEqualTo(feed.getUserId());
    }
  }
}
