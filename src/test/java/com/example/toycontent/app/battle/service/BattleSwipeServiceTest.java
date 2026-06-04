package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.battle.controller.dto.BattleSwipeRequest;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.NextItems;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.Result;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.SwipeAck;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleSwipe;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleSwipeRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.SwipeVerdict;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.common.voter.VoterId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleSwipeService")
class BattleSwipeServiceTest {

  private static final long BATTLE_ID = 1L;
  private static final long USER_ID = 500L;
  private static final String GUEST_ID = "guest-abc";

  @Mock private BattleRepository battleRepository;
  @Mock private BattleItemRepository battleItemRepository;
  @Mock private BattleSwipeRepository battleSwipeRepository;

  @InjectMocks private BattleSwipeService battleSwipeService;

  @Nested
  @DisplayName("swipe - 1건 등록")
  class Swipe {

    @Test
    @DisplayName("신규 등록: STRONG_PICK이면 strongPickCount가 1 증가하고 SwipeAck를 반환한다")
    void 신규_강추() {
      // given
      Battle battle = swipeBattle(BATTLE_ID, 3);
      BattleItem item = battle.getItems().get(0);
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.findById(item.getId())).willReturn(Optional.of(item));
      given(battleSwipeRepository.findByBattle_IdAndBattleItem_IdAndUserId(
          BATTLE_ID, item.getId(), USER_ID)).willReturn(Optional.empty());
      given(battleSwipeRepository.findSwipedItemIdsByUser(BATTLE_ID, USER_ID))
          .willReturn(List.of(item.getId()));

      // when
      SwipeAck ack = battleSwipeService.swipe(BATTLE_ID, VoterId.user(USER_ID),
          BattleSwipeRequest.Swipe.builder()
              .itemId(item.getId())
              .verdict(SwipeVerdict.STRONG_PICK)
              .build());

      // then
      assertThat(item.getStrongPickCount()).isEqualTo(1);
      assertThat(item.getPickCount()).isZero();
      assertThat(item.getPassCount()).isZero();
      assertThat(ack.getItemId()).isEqualTo(item.getId());
      assertThat(ack.getCompletedCount()).isEqualTo(1);
      assertThat(ack.getTotalCount()).isEqualTo(3);
      then(battleSwipeRepository).should().save(any());
    }

    @Test
    @DisplayName("게스트도 동일하게 신규 등록 가능하다")
    void 게스트_신규() {
      Battle battle = swipeBattle(BATTLE_ID, 2);
      BattleItem item = battle.getItems().get(0);
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.findById(item.getId())).willReturn(Optional.of(item));
      given(battleSwipeRepository.findByBattle_IdAndBattleItem_IdAndGuestId(
          BATTLE_ID, item.getId(), GUEST_ID)).willReturn(Optional.empty());
      given(battleSwipeRepository.findSwipedItemIdsByGuest(BATTLE_ID, GUEST_ID))
          .willReturn(List.of(item.getId()));

      battleSwipeService.swipe(BATTLE_ID, VoterId.guest(GUEST_ID),
          BattleSwipeRequest.Swipe.builder().itemId(item.getId()).verdict(SwipeVerdict.PICK).build());

      assertThat(item.getPickCount()).isEqualTo(1);
      then(battleSwipeRepository).should().save(any());
    }

    @Test
    @DisplayName("멱등 no-op: 기존과 동일 verdict로 재호출하면 카운터 변동 없음 + 새 row 저장 없음")
    void 동일_verdict_재호출_no_op() {
      Battle battle = swipeBattle(BATTLE_ID, 2);
      BattleItem item = battle.getItems().get(0);
      item.incrementStrongPickCount(); // 기존 상태 시뮬레이션
      BattleSwipe existing = BattleSwipe.ofUser(battle, item, USER_ID, SwipeVerdict.STRONG_PICK);

      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.findById(item.getId())).willReturn(Optional.of(item));
      given(battleSwipeRepository.findByBattle_IdAndBattleItem_IdAndUserId(
          BATTLE_ID, item.getId(), USER_ID)).willReturn(Optional.of(existing));
      given(battleSwipeRepository.findSwipedItemIdsByUser(BATTLE_ID, USER_ID))
          .willReturn(List.of(item.getId()));

      battleSwipeService.swipe(BATTLE_ID, VoterId.user(USER_ID),
          BattleSwipeRequest.Swipe.builder().itemId(item.getId())
              .verdict(SwipeVerdict.STRONG_PICK).build());

      assertThat(item.getStrongPickCount()).isEqualTo(1); // 변동 없음
      then(battleSwipeRepository).should(never()).save(any());
      assertThat(existing.getVerdict()).isEqualTo(SwipeVerdict.STRONG_PICK);
    }

    @Test
    @DisplayName("덮어쓰기: 다른 verdict로 재호출하면 기존 카운터 -1, 새 카운터 +1, row 갱신")
    void 다른_verdict_덮어쓰기() {
      Battle battle = swipeBattle(BATTLE_ID, 2);
      BattleItem item = battle.getItems().get(0);
      item.incrementStrongPickCount(); // 기존: STRONG_PICK 1건
      BattleSwipe existing = BattleSwipe.ofUser(battle, item, USER_ID, SwipeVerdict.STRONG_PICK);

      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.findById(item.getId())).willReturn(Optional.of(item));
      given(battleSwipeRepository.findByBattle_IdAndBattleItem_IdAndUserId(
          BATTLE_ID, item.getId(), USER_ID)).willReturn(Optional.of(existing));
      given(battleSwipeRepository.findSwipedItemIdsByUser(BATTLE_ID, USER_ID))
          .willReturn(List.of(item.getId()));

      battleSwipeService.swipe(BATTLE_ID, VoterId.user(USER_ID),
          BattleSwipeRequest.Swipe.builder().itemId(item.getId())
              .verdict(SwipeVerdict.PICK).build());

      assertThat(item.getStrongPickCount()).isZero();
      assertThat(item.getPickCount()).isEqualTo(1);
      assertThat(existing.getVerdict()).isEqualTo(SwipeVerdict.PICK);
      then(battleSwipeRepository).should(never()).save(any()); // 신규 row 저장 없음
    }

    @Test
    @DisplayName("SWIPE가 아닌 배틀(SINGLE/MULTIPLE)에서 호출하면 SWIPE_NOT_ALLOWED 에러")
    void SWIPE_타입_아니면_차단() {
      Battle battle = nonSwipeBattle();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));

      assertThatThrownBy(() -> battleSwipeService.swipe(BATTLE_ID, VoterId.user(USER_ID),
          BattleSwipeRequest.Swipe.builder().itemId(99L).verdict(SwipeVerdict.PICK).build()))
          .isInstanceOf(RestApiException.class)
          .hasMessageContaining(BattleErrorCode.SWIPE_NOT_ALLOWED.getMessage());
    }
  }

  @Nested
  @DisplayName("findNextItems - 미진행 아이템 조회")
  class FindNextItems {

    @Test
    @DisplayName("이미 스와이프한 아이템은 후보에서 제외된다")
    void 미진행만_반환() {
      Battle battle = swipeBattle(BATTLE_ID, 5);
      List<Long> allIds = battle.getItems().stream().map(BattleItem::getId).toList();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      // 첫 2개는 이미 스와이프 완료
      given(battleSwipeRepository.findSwipedItemIdsByUser(BATTLE_ID, USER_ID))
          .willReturn(List.of(allIds.get(0), allIds.get(1)));

      NextItems next = battleSwipeService.findNextItems(BATTLE_ID, VoterId.user(USER_ID), 10);

      assertThat(next.getItems()).hasSize(3);
      assertThat(next.getItems()).extracting("id")
          .doesNotContain(allIds.get(0), allIds.get(1));
      assertThat(next.getCompletedCount()).isEqualTo(2);
      assertThat(next.getTotalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("전부 완료한 voter에게도 next는 active 전체를 돌려준다 (재스와이프용)")
    void 전부_완료_재스와이프() {
      Battle battle = swipeBattle(BATTLE_ID, 2);
      List<Long> allIds = battle.getItems().stream().map(BattleItem::getId).toList();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleSwipeRepository.findSwipedItemIdsByUser(BATTLE_ID, USER_ID)).willReturn(allIds);

      NextItems next = battleSwipeService.findNextItems(BATTLE_ID, VoterId.user(USER_ID), 10);

      // 끝났음에도 active 전체 반환 — completedCount==totalCount로 "끝났음" 판단
      assertThat(next.getItems()).hasSize(2);
      assertThat(next.getCompletedCount()).isEqualTo(next.getTotalCount());
      assertThat(next.getCompletedCount()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("getResult - 결과 랭킹")
  class GetResult {

    @Test
    @DisplayName("strong*3 + pick*1 내림차순으로 정렬되고 rank가 1부터 부여된다")
    void 단순합산_정렬() {
      Battle battle = swipeBattle(BATTLE_ID, 3);
      List<BattleItem> items = battle.getItems();
      // 점수: items[0]=4(s=1,p=1), items[1]=7(s=2,p=1), items[2]=3(s=1,p=0)
      items.get(0).incrementStrongPickCount();
      items.get(0).incrementPickCount();
      items.get(1).incrementStrongPickCount();
      items.get(1).incrementStrongPickCount();
      items.get(1).incrementPickCount();
      items.get(2).incrementStrongPickCount();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));

      Result result = battleSwipeService.getResult(BATTLE_ID);

      assertThat(result.getItems()).hasSize(3);
      assertThat(result.getItems().get(0).getRank()).isEqualTo(1);
      assertThat(result.getItems().get(0).getId()).isEqualTo(items.get(1).getId());
      assertThat(result.getItems().get(0).getScore()).isEqualTo(7);
      assertThat(result.getItems().get(1).getId()).isEqualTo(items.get(0).getId());
      assertThat(result.getItems().get(1).getScore()).isEqualTo(4);
      assertThat(result.getItems().get(2).getId()).isEqualTo(items.get(2).getId());
      assertThat(result.getItems().get(2).getScore()).isEqualTo(3);
    }
  }

  // ==================== helpers ====================

  private Battle swipeBattle(Long battleId, int itemCount) {
    LocalDateTime now = LocalDateTime.now();
    Battle battle = Battle.builder()
        .id(battleId)
        .title("스와이프 배틀")
        .creatorId(100L)
        .startDate(now.minusDays(1))
        .participationStartDate(now.minusDays(1))
        .endDate(now.plusDays(7))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.SWIPE)
        .status(BattleStatus.NORMAL)
        .build();
    for (int i = 0; i < itemCount; i++) {
      battle.getItems().add(BattleItem.builder()
          .id(10L + i)
          .battle(battle)
          .itemType(BattleItemType.CUSTOM)
          .customName("아이템 " + i)
          .registerId(100L)
          .status(BattleItemStatus.ACTIVE)
          .build());
    }
    return battle;
  }

  private Battle nonSwipeBattle() {
    LocalDateTime now = LocalDateTime.now();
    return Battle.builder()
        .id(BATTLE_ID)
        .title("일반 투표 배틀")
        .creatorId(100L)
        .startDate(now.minusDays(1))
        .participationStartDate(now.minusDays(1))
        .endDate(now.plusDays(7))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.MULTIPLE)
        .status(BattleStatus.NORMAL)
        .build();
  }
}
