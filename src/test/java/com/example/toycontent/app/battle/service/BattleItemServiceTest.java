package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest.ItemRequest;
import com.example.toycontent.app.battle.controller.dto.BattleVoteRequest;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleItemEventEntry;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemEventEntryRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.common.voter.VoterId;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantResult;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.BattleFixture;
import com.example.toycontent.support.fixture.BattleItemFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleItemService")
class BattleItemServiceTest {

  private static final long BATTLE_ID = 1L;
  private static final long ITEM_ID_1 = 10L;
  private static final long ITEM_ID_2 = 20L;
  private static final long ITEM_ID_3 = 30L;
  private static final long VOTER_ID = 500L;
  private static final VoterId VOTER = VoterId.user(VOTER_ID);

  @Mock private BattleRepository battleRepository;
  @Mock private BattleItemRepository battleItemRepository;
  @Mock private BattleVoteRepository battleVoteRepository;
  @Mock private BattleItemCommentRepository battleItemCommentRepository;
  @Mock private BattleItemEventEntryRepository battleItemEventEntryRepository;
  @Mock private ProductRepository productRepository;
  @Mock private ExpGrantService expGrantService;
  @Mock private ExternalUserInfoService externalUserInfoService;
  @Mock private NotificationService notificationService;

  @InjectMocks private BattleItemService battleItemService;

  @Nested
  @DisplayName("vote - 투표 처리")
  class Vote {

    @Test
    @DisplayName("SINGLE 투표는 1개의 1순위만 허용하고 1점이 반영된다")
    void single_투표_정상() {
      // given
      Battle battle = singleBattle();
      BattleItem item = BattleItemFixture.custom(battle, "아이템 A");
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of());
      given(battleItemRepository.findById(ITEM_ID_1)).willReturn(Optional.of(item));

      // when
      battleItemService.vote(BATTLE_ID, VOTER,
          voteRequest(List.of(new int[]{Math.toIntExact(ITEM_ID_1), 1})));

      // then
      assertSoftly(softly -> {
        softly.assertThat(battle.getTotalParticipants())
            .as("참여자 수 증가").isEqualTo(1);
        softly.assertThat(battle.getTotalVotes())
            .as("투표 수 1").isEqualTo(1);
        softly.assertThat(battle.getTotalScore())
            .as("SINGLE은 1점").isEqualTo(1);
        softly.assertThat(item.getVoteCount())
            .as("아이템 득표 수").isEqualTo(1);
        softly.assertThat(item.getTotalScore())
            .as("아이템 점수 1점").isEqualTo(1);
      });
    }

    @Test
    @DisplayName("SINGLE 투표 재투표 시 기존 통계가 롤백되고 새 통계가 반영된다")
    void single_재투표_롤백_후_재반영() {
      // given
      Battle battle = singleBattle();
      BattleItem oldItem = BattleItemFixture.custom(battle, "기존 아이템");
      BattleItem newItem = BattleItemFixture.custom(battle, "신규 아이템");

      // 기존에 oldItem에 투표해둔 상태 시뮬레이션 (SINGLE은 1점)
      oldItem.incrementVoteCount();
      oldItem.addScore(1);
      battle.incrementTotalParticipants();
      battle.addTotalVotes(1);
      battle.addTotalScore(1);

      BattleVote existingVote = BattleVote.builder()
          .battle(battle).battleItem(oldItem).userId(VOTER_ID).voteRank(1).score(1).build();

      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of(existingVote));
      given(battleItemRepository.findById(ITEM_ID_2)).willReturn(Optional.of(newItem));

      // when - newItem으로 변경
      battleItemService.vote(BATTLE_ID, VOTER,
          voteRequest(List.of(new int[]{Math.toIntExact(ITEM_ID_2), 1})));

      // then
      assertSoftly(softly -> {
        softly.assertThat(oldItem.getVoteCount())
            .as("기존 아이템 득표 수 롤백").isZero();
        softly.assertThat(oldItem.getTotalScore())
            .as("기존 아이템 점수 롤백").isZero();
        softly.assertThat(newItem.getVoteCount())
            .as("신규 아이템 득표 수").isEqualTo(1);
        softly.assertThat(newItem.getTotalScore())
            .as("신규 아이템 점수 1점").isEqualTo(1);
        softly.assertThat(battle.getTotalParticipants())
            .as("참여자 수는 1 유지").isEqualTo(1);
        softly.assertThat(battle.getTotalVotes())
            .as("총 투표 수는 1 유지").isEqualTo(1);
        softly.assertThat(battle.getTotalScore())
            .as("배틀 총 점수 1점").isEqualTo(1);
      });
    }

    @Test
    @DisplayName("MULTIPLE 투표는 1/2/3위 순위로 각각 3/2/1점이 반영된다")
    void multiple_투표_정상() {
      // given
      Battle battle = BattleFixture.active(); // MULTIPLE
      BattleItem item1 = BattleItemFixture.custom(battle, "A");
      BattleItem item2 = BattleItemFixture.custom(battle, "B");
      BattleItem item3 = BattleItemFixture.custom(battle, "C");
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of());
      given(battleItemRepository.findById(ITEM_ID_1)).willReturn(Optional.of(item1));
      given(battleItemRepository.findById(ITEM_ID_2)).willReturn(Optional.of(item2));
      given(battleItemRepository.findById(ITEM_ID_3)).willReturn(Optional.of(item3));

      // when - 1/2/3위로 각각 투표
      battleItemService.vote(BATTLE_ID, VOTER, voteRequest(List.of(
          new int[]{Math.toIntExact(ITEM_ID_1), 1},
          new int[]{Math.toIntExact(ITEM_ID_2), 2},
          new int[]{Math.toIntExact(ITEM_ID_3), 3}
      )));

      // then
      assertSoftly(softly -> {
        softly.assertThat(battle.getTotalVotes())
            .as("투표한 아이템 수 = 3").isEqualTo(3);
        softly.assertThat(battle.getTotalScore())
            .as("3+2+1 = 6점").isEqualTo(6);
        softly.assertThat(item1.getTotalScore()).as("1위 3점").isEqualTo(3);
        softly.assertThat(item2.getTotalScore()).as("2위 2점").isEqualTo(2);
        softly.assertThat(item3.getTotalScore()).as("3위 1점").isEqualTo(1);
      });
    }

    @Test
    @DisplayName("MULTIPLE 투표에서 랭크가 연속되지 않으면 INVALID_RANK_SEQUENCE 예외를 던진다")
    void multiple_랭크_불연속_예외() {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of());

      // when & then - 1위, 3위만 (2위 누락)
      assertThatThrownBy(() -> battleItemService.vote(BATTLE_ID, VOTER, voteRequest(List.of(
          new int[]{Math.toIntExact(ITEM_ID_1), 1},
          new int[]{Math.toIntExact(ITEM_ID_3), 3}
      ))))
          .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("MULTIPLE 투표 재투표 시 기존 통계가 롤백되고 새 통계가 반영된다")
    void multiple_재투표_롤백_후_재반영() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem oldItem = BattleItemFixture.custom(battle, "기존 아이템");
      BattleItem newItem = BattleItemFixture.custom(battle, "신규 아이템");

      // 기존에 1위로 oldItem을 뽑아둔 상태 시뮬레이션
      oldItem.incrementVoteCount();
      oldItem.addScore(3);
      battle.incrementTotalParticipants();
      battle.addTotalVotes(1);
      battle.addTotalScore(3);

      BattleVote existingVote = BattleVote.builder()
          .battle(battle).battleItem(oldItem).userId(VOTER_ID).voteRank(1).score(3).build();

      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of(existingVote));
      given(battleItemRepository.findById(ITEM_ID_2)).willReturn(Optional.of(newItem));

      // when - newItem을 1위로 재투표
      battleItemService.vote(BATTLE_ID, VOTER, voteRequest(List.of(
          new int[]{Math.toIntExact(ITEM_ID_2), 1}
      )));

      // then - 기존 oldItem 통계 롤백, newItem 통계 반영
      assertSoftly(softly -> {
        softly.assertThat(oldItem.getVoteCount())
            .as("기존 아이템 득표 수 롤백").isZero();
        softly.assertThat(oldItem.getTotalScore())
            .as("기존 아이템 점수 롤백").isZero();
        softly.assertThat(newItem.getVoteCount())
            .as("신규 아이템 득표 수").isEqualTo(1);
        softly.assertThat(newItem.getTotalScore())
            .as("신규 아이템 점수 (1위=3점)").isEqualTo(3);
        softly.assertThat(battle.getTotalScore())
            .as("배틀 총 점수 = 3점").isEqualTo(3);
      });
    }

    @Test
    @DisplayName("비활성 상태의 아이템에 투표하면 INVALID_ITEM_STATUS 예외를 던진다")
    void 비활성_아이템_투표_예외() {
      // given
      Battle battle = singleBattle();
      BattleItem inactive = BattleItemFixture.withStatus(battle, BattleItemStatus.UNDER_REVIEW);
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of());
      given(battleItemRepository.findById(ITEM_ID_1)).willReturn(Optional.of(inactive));

      // when & then
      assertThatThrownBy(() -> battleItemService.vote(BATTLE_ID, VOTER,
          voteRequest(List.of(new int[]{Math.toIntExact(ITEM_ID_1), 1}))))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("cancelVote - 투표 취소")
  class CancelVote {

    @Test
    @DisplayName("투표가 있으면 통계를 롤백하고 삭제한다")
    void 정상_취소() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItemFixture.custom(battle, "아이템");
      item.incrementVoteCount();
      item.addScore(3);
      battle.incrementTotalParticipants();
      battle.addTotalVotes(1);
      battle.addTotalScore(3);

      BattleVote vote = BattleVote.builder()
          .battle(battle).battleItem(item).userId(VOTER_ID).voteRank(1).score(3).build();

      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattleAndUserId(battle, VOTER_ID))
          .willReturn(List.of(vote));

      // when
      battleItemService.cancelVote(BATTLE_ID, VOTER);

      // then
      assertSoftly(softly -> {
        softly.assertThat(battle.getTotalVotes()).isZero();
        softly.assertThat(battle.getTotalScore()).isZero();
        softly.assertThat(item.getVoteCount()).isZero();
        softly.assertThat(item.getTotalScore()).isZero();
      });
      then(battleVoteRepository).should().deleteAll(List.of(vote));
    }

    @Test
    @DisplayName("투표한 적이 없으면 VOTE_NOT_FOUND 예외를 던진다")
    void 투표_없음_예외() {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattleAndUserId(battle, VOTER_ID))
          .willReturn(List.of());

      // when & then
      assertThatThrownBy(() -> battleItemService.cancelVote(BATTLE_ID, VOTER))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("reportBattleItem - 아이템 신고")
  class ReportBattleItem {

    @Test
    @DisplayName("신고 시 아이템의 reportCount가 1 증가한다")
    void 신고수_증가() {
      // given
      Battle battle = BattleFixture.active();
      BattleItem item = BattleItemFixture.custom(battle, "아이템");
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.findById(ITEM_ID_1)).willReturn(Optional.of(item));

      // when
      battleItemService.reportBattleItem(BATTLE_ID, ITEM_ID_1, VOTER_ID,
          new com.example.toycontent.app.battle.controller.dto.BattleRequest.Report());

      // then
      assertThat(item.getReportCount()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("addBattleItems - eventId 처리")
  class AddBattleItems {

    private static final Long EVENT_BATTLE_ID = 21L;
    private static final Long CREATOR_ID = 100L;

    @Test
    @DisplayName("이벤트 배틀(21)에 eventId가 있으면 각 아이템마다 entry 1행씩 저장된다")
    void 이벤트_배틀_정상_저장() {
      // given
      Battle battle = eventBattle();
      List<ItemRequest> items = List.of(customItemReq("A"), customItemReq("B"));
      List<BattleItem> savedItems = List.of(
          savedItem(battle, 1001L, "A"),
          savedItem(battle, 1002L, "B"));

      given(battleRepository.findById(EVENT_BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.saveAll(anyList())).willReturn(savedItems);
      given(expGrantService.grantBattleItemAdd(anyLong(), anyLong()))
          .willReturn(ExpGrantResult.granted(10, 10, false));

      // when - actor == creator라 알림 분기 skip
      battleItemService.addBattleItems(EVENT_BATTLE_ID, CREATOR_ID,
          addRequest(items, "EVENT-2026"));

      // then
      ArgumentCaptor<List<BattleItemEventEntry>> captor = ArgumentCaptor.forClass(List.class);
      then(battleItemEventEntryRepository).should().saveAll(captor.capture());
      List<BattleItemEventEntry> saved = captor.getValue();
      assertSoftly(softly -> {
        softly.assertThat(saved).hasSize(2);
        softly.assertThat(saved).extracting(BattleItemEventEntry::getBattleItemId)
            .containsExactly(1001L, 1002L);
        softly.assertThat(saved).extracting(BattleItemEventEntry::getEventId)
            .containsOnly("EVENT-2026");
      });
    }

    @Test
    @DisplayName("이벤트 배틀(21)에 eventId가 null이면 entry 저장이 일어나지 않는다")
    void 이벤트_배틀_eventId_없으면_저장_skip() {
      // given
      Battle battle = eventBattle();
      List<ItemRequest> items = List.of(customItemReq("A"));
      given(battleRepository.findById(EVENT_BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.saveAll(anyList()))
          .willReturn(List.of(savedItem(battle, 1001L, "A")));
      given(expGrantService.grantBattleItemAdd(anyLong(), anyLong()))
          .willReturn(ExpGrantResult.granted(10, 10, false));

      // when
      battleItemService.addBattleItems(EVENT_BATTLE_ID, CREATOR_ID, addRequest(items, null));

      // then
      then(battleItemEventEntryRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("이벤트 배틀에 공백만 들어온 eventId는 미입력으로 취급되어 entry 저장이 일어나지 않는다")
    void 이벤트_배틀_eventId_공백이면_저장_skip() {
      // given
      Battle battle = eventBattle();
      given(battleRepository.findById(EVENT_BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.saveAll(anyList()))
          .willReturn(List.of(savedItem(battle, 1001L, "A")));
      given(expGrantService.grantBattleItemAdd(anyLong(), anyLong()))
          .willReturn(ExpGrantResult.granted(10, 10, false));

      // when
      battleItemService.addBattleItems(EVENT_BATTLE_ID, CREATOR_ID,
          addRequest(List.of(customItemReq("A")), "   "));

      // then
      then(battleItemEventEntryRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("일반 배틀에 eventId가 들어오면 EVENT_ID_NOT_ALLOWED 에러가 발생한다")
    void 일반_배틀_eventId_들어오면_에러() {
      // given - 화이트리스트(21)에 없는 배틀
      Battle battle = BattleFixture.active();  // id=1
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));

      // when/then
      assertThatThrownBy(() ->
          battleItemService.addBattleItems(BATTLE_ID, CREATOR_ID,
              addRequest(List.of(customItemReq("A")), "EVENT-2026")))
          .isInstanceOf(RestApiException.class)
          .hasMessageContaining(BattleErrorCode.EVENT_ID_NOT_ALLOWED.getMessage());

      then(battleItemEventEntryRepository).should(never()).saveAll(any());
      then(battleItemRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("일반 배틀에 eventId가 null이면 정상 처리되고 entry 저장이 일어나지 않는다")
    void 일반_배틀_eventId_없으면_정상() {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleItemRepository.saveAll(anyList()))
          .willReturn(List.of(savedItem(battle, 1001L, "A")));
      given(expGrantService.grantBattleItemAdd(anyLong(), anyLong()))
          .willReturn(ExpGrantResult.granted(10, 10, false));

      // when
      battleItemService.addBattleItems(BATTLE_ID, CREATOR_ID,
          addRequest(List.of(customItemReq("A")), null));

      // then
      then(battleItemEventEntryRepository).should(never()).saveAll(any());
    }

    private Battle eventBattle() {
      LocalDateTime now = LocalDateTime.now();
      return Battle.builder()
          .id(EVENT_BATTLE_ID)
          .title("이벤트 배틀")
          .creatorId(CREATOR_ID)
          .startDate(now.minusDays(1))
          .participationStartDate(now.minusDays(1))
          .endDate(now.plusDays(7))
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(VoteType.MULTIPLE)
          .status(BattleStatus.NORMAL)
          .build();
    }

    private ItemRequest customItemReq(String name) {
      return ItemRequest.builder()
          .itemType(BattleItemType.CUSTOM)
          .customName(name)
          .build();
    }

    private BattleItem savedItem(Battle battle, Long id, String name) {
      return BattleItem.builder()
          .id(id)
          .battle(battle)
          .itemType(BattleItemType.CUSTOM)
          .customName(name)
          .registerId(CREATOR_ID)
          .status(BattleItemStatus.ACTIVE)
          .build();
    }

    private BattleRequest.AddBattleItems addRequest(List<ItemRequest> items, String eventId) {
      return BattleRequest.AddBattleItems.builder()
          .items(items)
          .eventId(eventId)
          .build();
    }
  }

  // ==================== helpers ====================

  private Battle singleBattle() {
    LocalDateTime now = LocalDateTime.now();
    return Battle.builder()
        .id(BATTLE_ID)
        .title("SINGLE 배틀")
        .creatorId(100L)
        .startDate(now.minusDays(1))
        .participationStartDate(now.minusDays(1))
        .endDate(now.plusDays(7))
        .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
        .voteType(VoteType.SINGLE)
        .status(BattleStatus.NORMAL)
        .build();
  }

  private BattleVoteRequest.Vote voteRequest(List<int[]> itemRankPairs) {
    return BattleVoteRequest.Vote.builder()
        .votes(itemRankPairs.stream()
            .map(pair -> BattleVoteRequest.VoteItem.builder()
                .itemId((long) pair[0])
                .rank(pair[1])
                .build())
            .toList())
        .build();
  }
}
