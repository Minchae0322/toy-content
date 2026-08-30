package com.example.toycontent.app.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.FeedEvaluation;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import com.example.toycontent.app.feed.controller.dto.FeedResponse;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedHashtag;
import com.example.toycontent.app.feed.event.FeedViewedEvent;
import com.example.toycontent.app.feed.repository.FeedAttachmentFileRepository;
import com.example.toycontent.app.feed.repository.FeedHashtagRepository;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import com.example.toycontent.app.hashtag.repository.HashtagRepository;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.reward.exp.service.UserRewardService;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserFollowingService;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.FeedFixture;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService")
class FeedServiceTest {

  private static final long FEED_ID = 1L;
  private static final long FEED_OWNER_ID = 100L;
  private static final long OTHER_USER_ID = 200L;
  private static final long CATEGORY_ID = 10L;

  @Mock private FeedRepository feedRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private ProductRepository productRepository;
  @Mock private HashtagRepository hashtagRepository;
  @Mock private FeedAttachmentFileRepository feedAttachmentFileRepository;
  @Mock private ExternalUserInfoService externalUserInfoService;
  @Mock private ExternalUserFollowingService externalUserFollowingService;
  @Mock private FeedReactionRepository feedReactionRepository;
  @Mock private FeedHashtagRepository feedHashtagRepository;
  @Mock private ExpGrantService expGrantService;
  @Mock private UserRewardService userRewardService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private FeedQueryService feedQueryService;

  @InjectMocks private FeedService feedService;

  @Nested
  @DisplayName("getFeed - 단건 조회 (오케스트레이션)")
  class GetFeed {
    // DB 동작(조회수 불변·이벤트 발행·피드 없음 예외)은 FeedQueryServiceTest로 이사했다
    // (트랜잭션 경계 분리, 2026-08-30). 여기서는 로더 결과 + userInfo 후주입만 검증한다.

    @Test
    @DisplayName("로더가 만든 Detail에 userInfo를 트랜잭션 밖에서 채워 반환한다")
    void 로더_결과에_userInfo를_채운다() {
      // given
      Feed feed = FeedFixture.withId(FEED_ID);
      FeedResponse.Detail detail = FeedResponse.Detail.from(feed, null, List.of(), null);
      given(feedQueryService.loadDetail(FEED_ID, FEED_OWNER_ID))
          .willReturn(new FeedQueryService.DetailView(detail, feed.getUserId()));
      ExternalUserInfo userInfo =
          ExternalUserInfo.builder().userId(feed.getUserId()).nickname("작성자").build();
      given(externalUserInfoService.getUserInfo(feed.getUserId())).willReturn(userInfo);

      // when
      FeedResponse.Detail result = feedService.getFeed(FEED_ID, FEED_OWNER_ID);

      // then
      assertThat(result.getUserInfo()).isEqualTo(userInfo);
    }

    @Test
    @DisplayName("로더가 피드 없음 예외를 던지면 그대로 전파하고 외부 호출은 하지 않는다")
    void 피드_없음_예외() {
      // given
      given(feedQueryService.loadDetail(FEED_ID, FEED_OWNER_ID))
          .willThrow(new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

      // when & then
      assertThatThrownBy(() -> feedService.getFeed(FEED_ID, FEED_OWNER_ID))
          .isInstanceOf(RestApiException.class);

      then(externalUserInfoService).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("createFeed - 피드 생성")
  class CreateFeed {

    @Test
    @DisplayName("카테고리가 존재하지 않으면 RestApiException을 던진다")
    void 카테고리_없음_예외() {
      // given
      FeedRequest.CreateFeed request = createRequest();
      given(categoryRepository.findById(CATEGORY_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createFeed(request))
          .isInstanceOf(RestApiException.class);

      then(feedRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("해시태그가 처음 입력되면 Hashtag를 새로 저장하고 usageCount가 1로 올라간다")
    void 신규_해시태그_저장() {
      // given
      FeedRequest.CreateFeed request = createRequestWithHashtags(List.of("커피"));
      given(categoryRepository.findById(CATEGORY_ID))
          .willReturn(Optional.of(category()));
      given(feedRepository.save(any(Feed.class)))
          .willAnswer(invocation -> invocation.getArgument(0));
      given(hashtagRepository.findByName("커피")).willReturn(Optional.empty());
      given(hashtagRepository.save(any(Hashtag.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      feedService.createFeed(request);

      // then
      then(hashtagRepository).should().save(any(Hashtag.class));
    }

    @Test
    @DisplayName("이미 존재하는 해시태그면 새로 생성하지 않고 usageCount만 증가시킨다")
    void 기존_해시태그_재사용() {
      // given
      Hashtag existing = Hashtag.builder().name("커피").usageCount(5L).build();
      FeedRequest.CreateFeed request = createRequestWithHashtags(List.of("커피"));
      given(categoryRepository.findById(CATEGORY_ID))
          .willReturn(Optional.of(category()));
      given(feedRepository.save(any(Feed.class)))
          .willAnswer(invocation -> invocation.getArgument(0));
      given(hashtagRepository.findByName("커피")).willReturn(Optional.of(existing));

      // when
      feedService.createFeed(request);

      // then
      assertSoftly(softly -> {
        softly.assertThat(existing.getUsageCount())
            .as("기존 해시태그의 usageCount는 1 증가")
            .isEqualTo(6L);
      });
      then(hashtagRepository).should(never()).save(any());
    }
  }

  @Nested
  @DisplayName("updateFeed - 피드 수정")
  class UpdateFeed {

    @Test
    @DisplayName("작성자가 아닌 사용자가 수정을 시도하면 RestApiException을 던진다")
    void 권한_없는_수정_예외() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      FeedRequest.UpdateFeed request = updateRequest();
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

      // when & then
      assertThatThrownBy(() -> feedService.updateFeed(FEED_ID, request, OTHER_USER_ID))
          .isInstanceOf(RestApiException.class);

      then(feedHashtagRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 수정하면 기존 해시태그를 모두 삭제하고 새 해시태그로 교체한다")
    void 해시태그_교체() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      FeedRequest.UpdateFeed request = updateRequest();
      request.setHashtags(List.of("맛집"));
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(categoryRepository.findById(CATEGORY_ID))
          .willReturn(Optional.of(category()));
      given(hashtagRepository.findByName("맛집")).willReturn(Optional.empty());
      given(hashtagRepository.save(any(Hashtag.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      feedService.updateFeed(FEED_ID, request, FEED_OWNER_ID);

      // then
      then(feedHashtagRepository).should().deleteAllByFeed_Id(FEED_ID);
      then(hashtagRepository).should().save(any(Hashtag.class));
    }
  }

  @Nested
  @DisplayName("deleteFeed - 피드 삭제")
  class DeleteFeed {

    @Test
    @DisplayName("작성자가 삭제하면 isDeleted=true로 변경되고 해시태그 usageCount가 감소한다")
    void 정상_삭제() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      Hashtag hashtag = Hashtag.builder().name("커피").usageCount(10L).build();
      FeedHashtag.create(feed, hashtag); // setFeed()가 feed.hashtags에 자동 추가
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

      // when
      feedService.deleteFeed(FEED_ID, FEED_OWNER_ID, false);

      // then
      assertSoftly(softly -> {
        softly.assertThat(feed.getIsDeleted())
            .as("삭제 플래그").isTrue();
        softly.assertThat(hashtag.getUsageCount())
            .as("해시태그 사용 횟수는 1 감소").isEqualTo(9L);
      });
    }

    @Test
    @DisplayName("관리자는 작성자가 아니어도 피드를 삭제할 수 있다")
    void 관리자_삭제_허용() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      Hashtag hashtag = Hashtag.builder().name("커피").usageCount(10L).build();
      FeedHashtag.create(feed, hashtag);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

      // when
      feedService.deleteFeed(FEED_ID, OTHER_USER_ID, true);

      // then
      assertSoftly(softly -> {
        softly.assertThat(feed.getIsDeleted())
            .as("관리자 삭제 시에도 삭제 플래그는 true").isTrue();
        softly.assertThat(hashtag.getUsageCount())
            .as("해시태그 사용 횟수는 1 감소").isEqualTo(9L);
      });
    }

    @Test
    @DisplayName("작성자가 아니고 관리자도 아닌 사용자가 삭제를 시도하면 RestApiException을 던진다")
    void 권한_없는_삭제_예외() {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

      // when & then
      assertThatThrownBy(() -> feedService.deleteFeed(FEED_ID, OTHER_USER_ID, false))
          .isInstanceOf(RestApiException.class);

      assertThat(feed.getIsDeleted())
          .as("예외 시 삭제 플래그는 변경되지 않아야 한다").isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 피드를 삭제하면 RestApiException을 던진다")
    void 피드_없음_삭제_예외() {
      // given
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.deleteFeed(FEED_ID, FEED_OWNER_ID, false))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getFollowingFeeds - 팔로우 피드 조회")
  class GetFollowingFeeds {

    @Test
    @DisplayName("팔로잉한 유저가 없으면 빈 리스트를 반환한다")
    void 팔로우_없음_빈_결과() {
      // given
      com.example.toycontent.app.feed.controller.dto.FeedCondition.Following condition =
          new com.example.toycontent.app.feed.controller.dto.FeedCondition.Following();
      condition.setSize(10);
      given(externalUserFollowingService.getFollowingIds(FEED_OWNER_ID))
          .willReturn(List.of());
      given(feedRepository.findFollowingFeeds(any(), any()))
          .willReturn(List.of());
      given(externalUserInfoService.getUserInfos(any())).willReturn(Map.of());

      // when
      var response = feedService.getFollowingFeeds(condition, FEED_OWNER_ID);

      // then
      assertThat(response.getFeeds()).isEmpty();
    }
  }

  // ==================== helpers ====================

  private Category category() {
    return Category.builder().id(CATEGORY_ID).name("카페").build();
  }

  private FeedRequest.CreateFeed createRequest() {
    return createRequestWithHashtags(List.of());
  }

  private FeedRequest.CreateFeed createRequestWithHashtags(List<String> hashtags) {
    FeedRequest.CreateFeed request = new FeedRequest.CreateFeed();
    request.setUserId(FEED_OWNER_ID);
    request.setSubCategoryId(CATEGORY_ID);
    request.setReview("테스트 리뷰 내용");
    request.setEvaluation(FeedEvaluation.GOOD);
    request.setBuyPlace("테스트 구매처");
    request.setThumbnailAttachmentInfo(thumbnailInfo());
    request.setAttachmentFileInfos(List.of());
    request.setHashtags(hashtags);
    return request;
  }

  private FeedRequest.UpdateFeed updateRequest() {
    FeedRequest.UpdateFeed request = new FeedRequest.UpdateFeed();
    request.setUserId(FEED_OWNER_ID);
    request.setCategoryId(CATEGORY_ID);
    request.setReview("수정된 리뷰 내용");
    request.setEvaluation(FeedEvaluation.BEST);
    request.setBuyPlace("수정 구매처");
    request.setHashtags(List.of());
    return request;
  }

  private AttachmentFileRequest.AttachmentInfo thumbnailInfo() {
    return AttachmentFileRequest.AttachmentInfo.builder()
        .fileId(1L)
        .storedPath("/uploads/thumb.png")
        .originName("thumb.png")
        .contentType("image/png")
        .build();
  }
}
