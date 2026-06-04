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

    @Test
    @DisplayName("여러 배틀이 D-7 윈도우에 잡히면 각 생성자에게 모두 발송된다")
    void 다수_배틀_정상_발송() {
      // given
      Battle b1 = BattleFixture.active();
      Battle b2 = Battle.builder()
          .id(2L)
          .title("배틀2")
          .creatorId(200L)
          .startDate(LocalDateTime.now().minusDays(1))
          .participationStartDate(LocalDateTime.now().minusDays(1))
          .endDate(LocalDateTime.now().plusDays(7))
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.MULTIPLE)
          .status(BattleStatus.NORMAL)
          .build();
      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(b1, b2));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(b1.getId(), b2.getId()), BattleNotificationPhase.D7))
          .willReturn(List.of());

      // when
      scheduler.notifyD7();

      // then
      then(notificationService).should()
          .notifyBattleDeadlineOwnerD7(b1.getCreatorId(), b1.getId(), b1.getTitle());
      then(notificationService).should()
          .notifyBattleDeadlineOwnerD7(b2.getCreatorId(), b2.getId(), b2.getTitle());
      then(notificationService).should(times(2))
          .notifyBattleDeadlineOwnerD7(anyLong(), anyLong(), anyString());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("여러 배틀 중 일부만 발송 기록이 있으면 미발송 배틀만 발송된다")
    void 다수_배틀_일부_중복_방지() {
      // given
      Battle b1 = BattleFixture.active();
      Battle b2 = Battle.builder()
          .id(2L)
          .title("배틀2")
          .creatorId(200L)
          .startDate(LocalDateTime.now().minusDays(1))
          .participationStartDate(LocalDateTime.now().minusDays(1))
          .endDate(LocalDateTime.now().plusDays(7))
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.MULTIPLE)
          .status(BattleStatus.NORMAL)
          .build();
      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(b1, b2));
      // b1만 이미 발송됨
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(b1.getId(), b2.getId()), BattleNotificationPhase.D7))
          .willReturn(List.of(BattleNotificationSent.of(
              b1.getId(), BattleNotificationPhase.D7, b1.getCreatorId())));

      // when
      scheduler.notifyD7();

      // then - b1은 스킵, b2만 발송
      then(notificationService).should(never())
          .notifyBattleDeadlineOwnerD7(eq(b1.getCreatorId()), eq(b1.getId()), anyString());
      then(notificationService).should()
          .notifyBattleDeadlineOwnerD7(b2.getCreatorId(), b2.getId(), b2.getTitle());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(1);
      assertThat(captor.getValue().get(0).getBattleId()).isEqualTo(b2.getId());
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

    @Test
    @DisplayName("투표자가 없으면 생성자에게만 발송한다")
    void 투표자_없음_생성자만() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of());

      // when
      scheduler.notifyEnd();

      // then
      then(notificationService).should(times(1))
          .notifyBattleResult(creatorId, battle.getId(), battle.getTitle());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(1);
      assertThat(captor.getValue().get(0).getUserId()).isEqualTo(creatorId);
    }

    @Test
    @DisplayName("다수 투표자가 있으면 생성자 + 모든 투표자에게 발송된다")
    void 다수_투표자_정상_발송() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();
      Long v1 = 201L, v2 = 202L, v3 = 203L;

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of(v1, v2, v3));

      // when
      scheduler.notifyEnd();

      // then - 4명에게 발송 (creator + v1,v2,v3)
      then(notificationService).should(times(4))
          .notifyBattleResult(anyLong(), eq(battle.getId()), eq(battle.getTitle()));

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(4);
    }

    @Test
    @DisplayName("게스트(null)와 유효 유저가 섞이면 null만 제외하고 발송된다")
    void 게스트_혼합_null만_제외() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();
      Long voterId = 999L;

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(Arrays.asList(voterId, null));

      // when
      scheduler.notifyEnd();

      // then - creator + voter 2명
      then(notificationService).should()
          .notifyBattleResult(creatorId, battle.getId(), battle.getTitle());
      then(notificationService).should()
          .notifyBattleResult(voterId, battle.getId(), battle.getTitle());
      then(notificationService).should(times(2))
          .notifyBattleResult(anyLong(), eq(battle.getId()), eq(battle.getTitle()));

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("모든 대상자가 이미 발송되었으면 누구에게도 발송되지 않는다")
    void 전원_중복_방지() {
      // given
      Battle battle = endingBattle();
      Long creatorId = battle.getCreatorId();
      Long voterId = 999L;

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of(
              BattleNotificationSent.of(battle.getId(), BattleNotificationPhase.END, creatorId),
              BattleNotificationSent.of(battle.getId(), BattleNotificationPhase.END, voterId)));
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of(voterId));

      // when
      scheduler.notifyEnd();

      // then
      then(notificationService).should(never())
          .notifyBattleResult(anyLong(), anyLong(), anyString());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("여러 배틀이 동시에 종료되면 각 배틀의 생성자+투표자에게 발송된다")
    void 다수_배틀_정상_발송() {
      // given
      Battle b1 = endingBattle();
      Battle b2 = Battle.builder()
          .id(2L)
          .title("종료 배틀2")
          .creatorId(300L)
          .startDate(LocalDateTime.now().minusDays(7))
          .participationStartDate(LocalDateTime.now().minusDays(7))
          .endDate(LocalDateTime.now())
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.MULTIPLE)
          .status(BattleStatus.NORMAL)
          .build();

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(b1, b2));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(b1.getId(), b2.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(b1.getId()))
          .willReturn(List.of(401L));
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(b2.getId()))
          .willReturn(List.of(402L));

      // when
      scheduler.notifyEnd();

      // then - b1 creator + 401, b2 creator + 402 = 4건
      then(notificationService).should()
          .notifyBattleResult(b1.getCreatorId(), b1.getId(), b1.getTitle());
      then(notificationService).should()
          .notifyBattleResult(401L, b1.getId(), b1.getTitle());
      then(notificationService).should()
          .notifyBattleResult(b2.getCreatorId(), b2.getId(), b2.getTitle());
      then(notificationService).should()
          .notifyBattleResult(402L, b2.getId(), b2.getTitle());

      ArgumentCaptor<List<BattleNotificationSent>> captor = ArgumentCaptor.forClass(List.class);
      then(sentRepository).should().saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(4);
    }

    @Test
    @DisplayName("SWIPE 배틀 종료 시 1위 아이템명을 포함한 winner 알림으로 발송된다")
    void SWIPE_배틀_winner_알림() {
      // given - swipe 점수: A=4, B=9 (B가 1위), C=0
      Battle battle = swipeEndingBattle();
      com.example.toycontent.app.battle.domain.BattleItem a = swipeItem(battle, 10L, "맥북", 1, 1, 0); // 4
      com.example.toycontent.app.battle.domain.BattleItem b = swipeItem(battle, 20L, "키보드", 2, 3, 0); // 9
      com.example.toycontent.app.battle.domain.BattleItem c = swipeItem(battle, 30L, "마우스", 0, 0, 5); // 0
      battle.getItems().add(a);
      battle.getItems().add(b);
      battle.getItems().add(c);

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of());

      // when
      scheduler.notifyEnd();

      // then - 생성자에게 winner 메서드로 발송 (B가 1위)
      then(notificationService).should()
          .notifyBattleResultWithWinner(
              battle.getCreatorId(), battle.getId(), battle.getTitle(), "키보드");
      then(notificationService).should(never())
          .notifyBattleResult(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("SWIPE 배틀이라도 점수 0이면 winner 없음 → 기존 BATTLE_RESULT 메시지로 폴백")
    void SWIPE_배틀_점수_0이면_폴백() {
      // given - 모든 아이템 swipe 점수 0
      Battle battle = swipeEndingBattle();
      battle.getItems().add(swipeItem(battle, 10L, "맥북", 0, 0, 0));

      given(battleRepository.findByEndDateBetween(any(), any()))
          .willReturn(List.of(battle));
      given(sentRepository.findByBattleIdInAndPhase(
          List.of(battle.getId()), BattleNotificationPhase.END))
          .willReturn(List.of());
      given(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()))
          .willReturn(List.of());

      // when
      scheduler.notifyEnd();

      // then
      then(notificationService).should()
          .notifyBattleResult(battle.getCreatorId(), battle.getId(), battle.getTitle());
      then(notificationService).should(never())
          .notifyBattleResultWithWinner(anyLong(), anyLong(), anyString(), anyString());
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

    private Battle swipeEndingBattle() {
      LocalDateTime now = LocalDateTime.now();
      return Battle.builder()
          .id(BattleFixture.DEFAULT_BATTLE_ID)
          .title("스와이프 배틀")
          .creatorId(BattleFixture.DEFAULT_CREATOR_ID)
          .startDate(now.minusDays(7))
          .participationStartDate(now.minusDays(7))
          .endDate(now)
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.SWIPE)
          .status(BattleStatus.NORMAL)
          .build();
    }

    private com.example.toycontent.app.battle.domain.BattleItem swipeItem(
        Battle battle, Long id, String name, int strong, int pick, int pass) {
      return com.example.toycontent.app.battle.domain.BattleItem.builder()
          .id(id)
          .battle(battle)
          .itemType(com.example.toycontent.app.common.enumuration.BattleItemType.CUSTOM)
          .customName(name)
          .registerId(100L)
          .status(com.example.toycontent.app.common.enumuration.BattleItemStatus.ACTIVE)
          .strongPickCount(strong)
          .pickCount(pick)
          .passCount(pass)
          .build();
    }
  }
}
