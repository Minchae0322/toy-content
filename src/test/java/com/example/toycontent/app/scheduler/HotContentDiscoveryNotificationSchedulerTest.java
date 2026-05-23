package com.example.toycontent.app.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.notification.hotcontent.HotContentCandidate;
import com.example.toycontent.app.notification.hotcontent.HotContentNotificationSent;
import com.example.toycontent.app.notification.hotcontent.HotContentNotificationSentRepository;
import com.example.toycontent.app.notification.hotcontent.HotContentSource;
import com.example.toycontent.app.notification.hotcontent.HotContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotContentDiscoveryNotificationScheduler")
class HotContentDiscoveryNotificationSchedulerTest {

  @Mock private HotContentNotificationSentRepository sentRepository;
  @Mock private NotificationService notificationService;
  @Mock private HotContentSource feedSource;
  @Mock private HotContentSource battleSource;

  private HotContentDiscoveryNotificationScheduler scheduler;

  private void setupSchedulerWith(List<HotContentSource> sources) {
    scheduler = new HotContentDiscoveryNotificationScheduler(
        sources, sentRepository, notificationService);
  }

  @Test
  @DisplayName("어떤 Source에서도 후보가 없으면 발송하지 않는다")
  void 후보_없음() {
    // given
    given(feedSource.findTopCandidates(anyInt())).willReturn(List.of());
    given(battleSource.findTopCandidates(anyInt())).willReturn(List.of());
    setupSchedulerWith(List.of(feedSource, battleSource));

    // when
    scheduler.notifyDailyHotContent();

    // then
    then(notificationService).should(never()).broadcastHotContentDiscovery(any());
    then(sentRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("Feed/Battle 후보 중 hotScore가 가장 높은 1건만 브로드캐스트되고 sent 기록된다")
  void 최상위_1건_선정() {
    // given - battle이 더 hot
    HotContentCandidate feedTop = candidate(HotContentType.FEED, 10L, "맥북M5", 50.0);
    HotContentCandidate battleTop = candidate(HotContentType.BATTLE, 20L, "키보드 대전", 80.0);
    given(feedSource.findTopCandidates(anyInt())).willReturn(List.of(feedTop));
    given(battleSource.findTopCandidates(anyInt())).willReturn(List.of(battleTop));
    given(sentRepository.findByContentTypeAndContentIdIn(any(), anyCollection()))
        .willReturn(List.of());
    setupSchedulerWith(List.of(feedSource, battleSource));

    // when
    scheduler.notifyDailyHotContent();

    // then - battle 1건만 발송
    ArgumentCaptor<HotContentCandidate> captor = ArgumentCaptor.forClass(HotContentCandidate.class);
    then(notificationService).should().broadcastHotContentDiscovery(captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(HotContentType.BATTLE);
    assertThat(captor.getValue().getContentId()).isEqualTo(20L);

    ArgumentCaptor<HotContentNotificationSent> sentCaptor =
        ArgumentCaptor.forClass(HotContentNotificationSent.class);
    then(sentRepository).should().save(sentCaptor.capture());
    assertThat(sentCaptor.getValue().getContentType()).isEqualTo(HotContentType.BATTLE);
    assertThat(sentCaptor.getValue().getContentId()).isEqualTo(20L);
  }

  @Test
  @DisplayName("최상위 후보가 이미 발송됐으면 다음 hotScore 후보로 넘어간다")
  void 기_발송_제외() {
    // given - battle 1위 / feed 2위 / battle 3위, battle 1위는 기 발송
    HotContentCandidate battleTop = candidate(HotContentType.BATTLE, 20L, "키보드 대전", 80.0);
    HotContentCandidate feedTop = candidate(HotContentType.FEED, 10L, "맥북M5", 60.0);
    HotContentCandidate battleSecond = candidate(HotContentType.BATTLE, 21L, "마우스 대전", 30.0);
    given(feedSource.findTopCandidates(anyInt())).willReturn(List.of(feedTop));
    given(battleSource.findTopCandidates(anyInt()))
        .willReturn(List.of(battleTop, battleSecond));
    given(sentRepository.findByContentTypeAndContentIdIn(eq(HotContentType.BATTLE), anyCollection()))
        .willReturn(List.of(HotContentNotificationSent.of(HotContentType.BATTLE, 20L)));
    given(sentRepository.findByContentTypeAndContentIdIn(eq(HotContentType.FEED), anyCollection()))
        .willReturn(List.of());
    setupSchedulerWith(List.of(feedSource, battleSource));

    // when
    scheduler.notifyDailyHotContent();

    // then - feed (2위, 60.0)가 선정
    ArgumentCaptor<HotContentCandidate> captor = ArgumentCaptor.forClass(HotContentCandidate.class);
    then(notificationService).should().broadcastHotContentDiscovery(captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(HotContentType.FEED);
    assertThat(captor.getValue().getContentId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("모든 후보가 이미 발송됐으면 발송하지 않는다")
  void 후보_전원_기_발송() {
    // given
    HotContentCandidate feedTop = candidate(HotContentType.FEED, 10L, "맥북M5", 50.0);
    HotContentCandidate battleTop = candidate(HotContentType.BATTLE, 20L, "키보드 대전", 80.0);
    given(feedSource.findTopCandidates(anyInt())).willReturn(List.of(feedTop));
    given(battleSource.findTopCandidates(anyInt())).willReturn(List.of(battleTop));
    given(sentRepository.findByContentTypeAndContentIdIn(eq(HotContentType.FEED), anyCollection()))
        .willReturn(List.of(HotContentNotificationSent.of(HotContentType.FEED, 10L)));
    given(sentRepository.findByContentTypeAndContentIdIn(eq(HotContentType.BATTLE), anyCollection()))
        .willReturn(List.of(HotContentNotificationSent.of(HotContentType.BATTLE, 20L)));
    setupSchedulerWith(List.of(feedSource, battleSource));

    // when
    scheduler.notifyDailyHotContent();

    // then
    then(notificationService).should(never()).broadcastHotContentDiscovery(any());
    then(sentRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("한 종류 Source만 후보가 있어도 정상 발송된다")
  void 단일_Source_정상_발송() {
    // given - feed만 후보, battle은 비어 있음
    HotContentCandidate feedTop = candidate(HotContentType.FEED, 10L, "맥북M5", 50.0);
    given(feedSource.findTopCandidates(anyInt())).willReturn(List.of(feedTop));
    given(battleSource.findTopCandidates(anyInt())).willReturn(List.of());
    given(sentRepository.findByContentTypeAndContentIdIn(eq(HotContentType.FEED), anyCollection()))
        .willReturn(List.of());
    setupSchedulerWith(List.of(feedSource, battleSource));

    // when
    scheduler.notifyDailyHotContent();

    // then
    ArgumentCaptor<HotContentCandidate> captor = ArgumentCaptor.forClass(HotContentCandidate.class);
    then(notificationService).should().broadcastHotContentDiscovery(captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(HotContentType.FEED);
  }

  private static HotContentCandidate candidate(
      HotContentType type, Long id, String name, double score) {
    return HotContentCandidate.builder()
        .type(type)
        .contentId(id)
        .displayName(name)
        .hotScore(score)
        .build();
  }
}
