package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.battle.controller.dto.BattleVoteRequest;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.support.fixture.BattleFixture;
import com.example.toycontent.support.fixture.BattleItemFixture;
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
@DisplayName("BattleItemService")
class BattleItemServiceTest {

  private static final long BATTLE_ID = 1L;
  private static final long ITEM_ID_1 = 10L;
  private static final long ITEM_ID_2 = 20L;
  private static final long ITEM_ID_3 = 30L;
  private static final long VOTER_ID = 500L;

  @Mock private BattleRepository battleRepository;
  @Mock private BattleItemRepository battleItemRepository;
  @Mock private BattleVoteRepository battleVoteRepository;
  @Mock private ProductRepository productRepository;

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
      battleItemService.vote(BATTLE_ID, VOTER_ID,
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
    @DisplayName("SINGLE 투표에서 이미 투표한 경우 ALREADY_VOTED 예외를 던진다")
    void single_재투표_예외() {
      // given
      Battle battle = singleBattle();
      BattleVote existingVote = BattleVote.builder().battle(battle).userId(VOTER_ID).voteRank(1).score(1).build();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleVoteRepository.findByBattle_IdAndUserId(BATTLE_ID, VOTER_ID))
          .willReturn(List.of(existingVote));

      // when & then
      assertThatThrownBy(() -> battleItemService.vote(BATTLE_ID, VOTER_ID,
          voteRequest(List.of(new int[]{Math.toIntExact(ITEM_ID_1), 1}))))
          .isInstanceOf(RestApiException.class);

      then(battleVoteRepository).should(never()).saveAll(any());
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
      battleItemService.vote(BATTLE_ID, VOTER_ID, voteRequest(List.of(
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
      assertThatThrownBy(() -> battleItemService.vote(BATTLE_ID, VOTER_ID, voteRequest(List.of(
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
      battleItemService.vote(BATTLE_ID, VOTER_ID, voteRequest(List.of(
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
      assertThatThrownBy(() -> battleItemService.vote(BATTLE_ID, VOTER_ID,
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
      battleItemService.cancelVote(BATTLE_ID, VOTER_ID);

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
      assertThatThrownBy(() -> battleItemService.cancelVote(BATTLE_ID, VOTER_ID))
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
