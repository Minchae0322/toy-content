package com.example.toycontent.app.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.common.enumuration.ReportReason;
import com.example.toycontent.app.common.enumuration.ReportStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedReportRequest;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReport;
import com.example.toycontent.app.feed.repository.FeedReportRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.support.fixture.FeedFixture;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedReportService")
class FeedReportServiceTest {

  private static final long FEED_ID = 1L;
  private static final long FEED_OWNER_ID = 100L;
  private static final long REPORTER_ID = 200L;
  private static final long REPORT_ID = 999L;

  @Mock private FeedRepository feedRepository;
  @Mock private FeedReportRepository feedReportRepository;

  @InjectMocks private FeedReportService feedReportService;

  private FeedReportRequest request(ReportReason reason, String detail) {
    return new FeedReportRequest(reason, detail);
  }

  @Nested
  @DisplayName("report")
  class Report {

    @Test
    @DisplayName("정상 신고 시 FeedReport가 저장되고 PENDING 상태로 생성된다")
    void 정상_신고() throws Exception {
      // given
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(feedReportRepository.existsByFeedIdAndReporterId(FEED_ID, REPORTER_ID))
          .willReturn(false);
      given(feedReportRepository.save(any(FeedReport.class)))
          .willAnswer(invocation -> {
            FeedReport saved = invocation.getArgument(0);
            setId(saved, REPORT_ID);
            return saved;
          });

      // when
      Long savedId = feedReportService.report(FEED_ID, REPORTER_ID,
          request(ReportReason.SPAM, "광고성"));

      // then
      ArgumentCaptor<FeedReport> captor = ArgumentCaptor.forClass(FeedReport.class);
      then(feedReportRepository).should().save(captor.capture());
      FeedReport saved = captor.getValue();

      assertSoftly(softly -> {
        softly.assertThat(savedId).isEqualTo(REPORT_ID);
        softly.assertThat(saved.getFeed()).isSameAs(feed);
        softly.assertThat(saved.getReporterId()).isEqualTo(REPORTER_ID);
        softly.assertThat(saved.getReason()).isEqualTo(ReportReason.SPAM);
        softly.assertThat(saved.getDetail()).isEqualTo("광고성");
        softly.assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
      });
    }

    @Test
    @DisplayName("detail은 null이어도 신고가 성립한다")
    void detail_없이_신고() throws Exception {
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(feedReportRepository.existsByFeedIdAndReporterId(FEED_ID, REPORTER_ID))
          .willReturn(false);
      given(feedReportRepository.save(any(FeedReport.class)))
          .willAnswer(invocation -> {
            FeedReport saved = invocation.getArgument(0);
            setId(saved, REPORT_ID);
            return saved;
          });

      Long savedId = feedReportService.report(FEED_ID, REPORTER_ID,
          request(ReportReason.OFFENSIVE, null));

      assertThat(savedId).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("피드가 존재하지 않으면 FEED_NOT_FOUND 예외가 발생한다")
    void 피드_없음() {
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> feedReportService.report(FEED_ID, REPORTER_ID,
          request(ReportReason.SPAM, null)))
          .isInstanceOf(RestApiException.class)
          .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_NOT_FOUND);

      then(feedReportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("본인이 작성한 피드는 신고할 수 없다 (FEED_REPORT_SELF)")
    void 본인_피드_신고_불가() {
      Feed feed = FeedFixture.withUserId(REPORTER_ID); // owner == reporter
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));

      assertThatThrownBy(() -> feedReportService.report(FEED_ID, REPORTER_ID,
          request(ReportReason.SPAM, null)))
          .isInstanceOf(RestApiException.class)
          .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_REPORT_SELF);

      then(feedReportRepository).should(never())
          .existsByFeedIdAndReporterId(any(), any());
      then(feedReportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 신고한 피드를 다시 신고하면 FEED_REPORT_DUPLICATED 예외가 발생한다")
    void 중복_신고_불가() {
      Feed feed = FeedFixture.withUserId(FEED_OWNER_ID);
      given(feedRepository.findById(FEED_ID)).willReturn(Optional.of(feed));
      given(feedReportRepository.existsByFeedIdAndReporterId(FEED_ID, REPORTER_ID))
          .willReturn(true);

      assertThatThrownBy(() -> feedReportService.report(FEED_ID, REPORTER_ID,
          request(ReportReason.SPAM, null)))
          .isInstanceOf(RestApiException.class)
          .hasFieldOrPropertyWithValue("errorCode", FeedErrorCode.FEED_REPORT_DUPLICATED);

      then(feedReportRepository).should(never()).save(any());
    }
  }

  /**
   * 테스트에서 save 시점의 ID 부여를 흉내내기 위해 BaseReport.id를 리플렉션으로 설정.
   */
  private static void setId(FeedReport report, Long id) throws Exception {
    Field idField = report.getClass().getSuperclass().getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(report, id);
  }
}
