package com.example.toycontent.app.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.notification.BattleNotificationPhase;
import com.example.toycontent.app.notification.BattleNotificationSent;
import com.example.toycontent.app.notification.BattleNotificationSentRepository;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.support.fixture.BattleFixture;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleDeadlineNotificationScheduler")
class BattleDeadlineNotificationSchedulerTest {

  @Mock private BattleRepository battleRepository;
  @Mock private BattleVoteRepository battleVoteRepository;
  @Mock private BattleNotificationSentRepository sentRepository;
  @Mock private NotificationService notificationService;

  @InjectMocks private BattleDeadlineNotificationScheduler scheduler;

  @Nested
  @DisplayName("notifyD7 - D-7 생성자 알림")
  class NotifyD7 {

    @Test
    @DisplayName("D-7 대상 배틀이 없으면 발송하지 않는다")
    void 대상_없음() {
      // given
      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of());

      // when
      scheduler.notifyD7();

      // then
      then(notificationService).should(never())
          .notifyBattleDeadlineOwnerD7(anyLong(), anyLong(), anyString());
      then(sentRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("D-7 대상 배틀의 생성자에게 알림이 발행되고 sent 테이블에 일괄 저장된다")
    void 정상_발송() {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.D7))
          .willReturn(List.of());

      // when
      scheduler.notifyD7();

      // then
      then(notificationService).should().notifyBattleDeadlineOwnerD7(
          battle.getCreatorId(), battle.getId(), battle.getTitle());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      List<BattleNotificationSent> saved = captor.getValue();
      assertThat(saved).hasSize(1);
      assertThat(saved.get(0).getBattleId()).isEqualTo(battle.getId());
      assertThat(saved.get(0).getPhase()).isEqualTo(BattleNotificationPhase.D7);
      assertThat(saved.get(0).getUserId()).isEqualTo(battle.getCreatorId());
    }

    @Test
    @DisplayName("이미 D-7 발송 기록이 있으면 재발송하지 않는다")
    void 중복_발송_방지() {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.D7))
          .willReturn(List.of(BattleNotificationSent.of(
              battle.getId(), BattleNotificationPhase.D7, battle.getCreatorId())));

      // when
      scheduler.notifyD7();

      // then
      then(notificationService).should(never())
          .notifyBattleDeadlineOwnerD7(anyLong(), anyLong(), anyString());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).isEmpty();
    }
  }

  @Nested
  @DisplayName("notifyEnd - 종료 알림")
  class NotifyEnd {

    @Test
    @DisplayName("종료 대상 배틀이 없으면 발송하지 않는다")
    void 대상_없음() {
      // given
      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of());

      // when
      scheduler.notifyEnd();

      // then
      then(notificationService).should(never())
          .notifyBattleResult(anyLong(), anyLong(), anyString());
      then(sentRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("생성자와 투표자에게 결과 알림이 발행되고, 생성자=투표자인 경우 1건으로 dedup된다")
    void 생성자_투표자_중복_제거() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();
      Long otherVoterId = 999L;

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      // creatorId가 투표자에도 포함된 케이스 — Set으로 dedup되어야 함
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of(creatorId, otherVoterId));

      // when
      scheduler.notifyEnd();

      // then - 정확히 2명에게만 발송 (creatorId 한 번, otherVoterId 한 번)
      then(notificationService).should()
          .notifyBattleResult(creatorId, battle.getId(), battle.getTitle());
      then(notificationService).should()
          .notifyBattleResult(otherVoterId, battle.getId(), battle.getTitle());
      then(notificationService).should(times(2))
          .notifyBattleResult(anyLong(), eq(battle.getId()), eq(battle.getTitle()));

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("이미 발송된 유저는 건너뛴다")
    void 일부_유저_중복_방지() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();
      Long voterId = 999L;

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      // creator는 이미 발송됨, voter는 미발송
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of(BattleNotificationSent.of(
              battle.getId(), BattleNotificationPhase.END, creatorId)));
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of(voterId));

      // when
      scheduler.notifyEnd();

      // then - voter 1명에게만 발송
      then(notificationService).should(never())
          .notifyBattleResult(eq(creatorId), anyLong(), anyString());
      then(notificationService).should()
          .notifyBattleResult(voterId, battle.getId(), battle.getTitle());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("게스트(userId null) 투표자는 알림 대상에서 제외된다")
    void 게스트_제외() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(Arrays.asList((Long) null));

      // when
      scheduler.notifyEnd();

      // then - 생성자에게만 발송 (null 무시)
      then(notificationService).should(times(1))
          .notifyBattleResult(creatorId, battle.getId(), battle.getTitle());
    }

    private Battle endingBattle() {
      LocalDateTime now = LocalDateTime.now();
      return Battle.builder()
          .id(BattleFixture.DEFAULT_BATTLE_ID)
          .title("종료 배틀")
          .creatorId(BattleFixture.DEFAULT_CREATOR_ID)
          .startDate(now.minusDays(7))
          .participationStartDate(now.minusDays(7))
          .endDate(now)
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.MULTIPLE)
          .status(BattleStatus.NORMAL)
          .build();
    }
  }
}
