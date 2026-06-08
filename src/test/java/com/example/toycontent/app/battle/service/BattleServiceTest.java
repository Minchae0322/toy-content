package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.toycontent.app.battle.controller.dto.BattleRequest.BattleItemsSearchCondition;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleHotItem;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleHotList;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.repository.BattleAttachmentFileRepository;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.exp.service.UserRewardService;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.BattleFixture;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService")
class BattleServiceTest {

  private static final long BATTLE_ID = 1L;
  private static final long CREATOR_ID = 100L;
  private static final long OTHER_USER_ID = 200L;

  @Mock private BattleItemService battleItemService;
  @Mock private BattleRepository battleRepository;
  @Mock private BattleItemRepository battleItemRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private ExternalUserInfoService externalUserInfoService;
  @Mock private BattleAttachmentFileRepository battleAttachmentFileRepository;
  @Mock private BattleVoteRepository battleVoteRepository;
  @Mock private BattleItemCommentRepository battleItemCommentRepository;
  @Mock private ExpGrantService expGrantService;
  @Mock private UserRewardService userRewardService;

  @InjectMocks private BattleService battleService;

  @Nested
  @DisplayName("validateCreation - 배틀 생성 제한 검증")
  class ValidateCreation {

    @Test
    @DisplayName("현재 진행중 배틀이 10개 미만이고 24시간 내 생성이 3개 미만이면 검증을 통과한다")
    void 정상_생성_가능() {
      // given
      given(battleRepository.countOngoingByCreatorId(eq(CREATOR_ID), any()))
          .willReturn(5L);
      given(battleRepository.countByCreatorIdAndCreatedAtAfter(eq(CREATOR_ID), any()))
          .willReturn(1L);

      // expect - 예외 없이 실행
      battleService.validateCreation(CREATOR_ID);
    }

    @Test
    @DisplayName("현재 진행중 배틀이 10개 이상이면 MAX_ACTIVE_BATTLES 예외를 던진다")
    void 활성_배틀_초과_예외() {
      // given
      given(battleRepository.countOngoingByCreatorId(eq(CREATOR_ID), any()))
          .willReturn(10L);

      // when & then
      assertThatThrownBy(() -> battleService.validateCreation(CREATOR_ID))
          .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("24시간 내 생성이 3개 이상이면 DAILY_LIMIT 예외를 던진다")
    void 일일_생성_초과_예외() {
      // given
      given(battleRepository.countOngoingByCreatorId(eq(CREATOR_ID), any()))
          .willReturn(2L);
      given(battleRepository.countByCreatorIdAndCreatedAtAfter(eq(CREATOR_ID), any()))
          .willReturn(3L);

      // when & then
      assertThatThrownBy(() -> battleService.validateCreation(CREATOR_ID))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getBattleDetail - 상세 조회")
  class GetBattleDetail {

    @Test
    @DisplayName("상세 조회 시 배틀의 totalViews가 1 증가한다")
    void 조회수_증가() {
      // given
      Battle battle = BattleFixture.active();
      int previousViews = battle.getTotalViews();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(externalUserInfoService.getUserInfo(battle.getCreatorId()))
          .willReturn(ExternalUserInfo.builder().userId(battle.getCreatorId()).nickname("작성자").build());

      // when
      battleService.getBattleDetail(BATTLE_ID, OTHER_USER_ID, false);

      // then
      assertThat(battle.getTotalViews())
          .as("조회 이후 조회수")
          .isEqualTo(previousViews + 1);
    }

    @Test
    @DisplayName("존재하지 않는 배틀 조회 시 RestApiException을 던진다")
    void 배틀_없음_예외() {
      // given
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> battleService.getBattleDetail(BATTLE_ID, OTHER_USER_ID, false))
          .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("아이템 조회는 BattleItemService 에 ACTIVE 필터와 isAdmin 값을 그대로 전달한다")
    void 아이템_조회_위임() {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(externalUserInfoService.getUserInfo(battle.getCreatorId()))
          .willReturn(ExternalUserInfo.builder().userId(battle.getCreatorId()).nickname("작성자").build());

      // when - 어드민이 조회
      battleService.getBattleDetail(BATTLE_ID, OTHER_USER_ID, true);

      // then - BattleItemService 로 위임하며 status=ACTIVE, isAdmin=true 전달
      then(battleItemService).should().getBattleItems(
          eq(BATTLE_ID),
          eq(OTHER_USER_ID),
          eq(true),
          argThat((BattleItemsSearchCondition cond) ->
              cond != null && cond.getStatus() == BattleItemStatus.ACTIVE));
    }
  }

  @Nested
  @DisplayName("getHotBattleList - 핫 배틀 목록")
  class GetHotBattleList {

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("SWIPE 배틀: topItems가 swipe 점수 기준으로 정렬되고 totalScore/votePercentage가 swipe 값으로 채워진다")
    void SWIPE_배틀_swipe_점수_노출() {
      // given - swipe 점수: A=4(s=1,p=1), B=9(s=2,p=3), C=1(s=0,p=1). 합 14
      Battle battle = swipeBattle(21L);
      BattleItem a = swipeItem(battle, 100L, "A", 1, 1, 0);
      BattleItem b = swipeItem(battle, 200L, "B", 2, 3, 0);
      BattleItem c = swipeItem(battle, 300L, "C", 0, 1, 0);

      BattleHotList hot = BattleHotList.builder().id(21L).voteType(VoteType.SWIPE).build();
      given(battleRepository.findHotBattlesWithSearchCondition(pageable))
          .willReturn(new PageImpl<>(List.of(hot), pageable, 1));
      given(battleItemRepository.findByBattleIdInAndStatusOrderByTotalScoreDesc(
          List.of(21L), BattleItemStatus.ACTIVE))
          .willReturn(List.of(a, b, c));

      // when
      Page<BattleHotList> result = battleService.getHotBattleList(pageable);

      // then
      List<BattleHotItem> topItems = result.getContent().get(0).getTopItems();
      assertThat(topItems).hasSize(3);
      assertThat(topItems.get(0).getId()).isEqualTo(200L);  // B (9)
      assertThat(topItems.get(0).getTotalScore()).isEqualTo(9);
      assertThat(topItems.get(0).getVotePercentage()).isEqualTo(9.0 / 14.0);
      assertThat(topItems.get(0).getRank()).isEqualTo(1);

      assertThat(topItems.get(1).getId()).isEqualTo(100L);  // A (4)
      assertThat(topItems.get(1).getTotalScore()).isEqualTo(4);

      assertThat(topItems.get(2).getId()).isEqualTo(300L);  // C (1)
      assertThat(topItems.get(2).getTotalScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("vote 배틀: topItems는 totalScore 기준으로 정렬되고 swipe 카운터는 사용되지 않는다")
    void VOTE_배틀_totalScore_노출() {
      // given - swipe 점수는 일부러 vote와 거꾸로(=무시되어야 함)
      Battle battle = voteBattle(22L);
      battle.addTotalScore(10);
      BattleItem a = voteItem(battle, 100L, "A", 7);   // swipe 0
      BattleItem b = voteItem(battle, 200L, "B", 3);   // swipe 0
      a.incrementStrongPickCount(); // swipe 점수는 3이지만 voteType이 MULTIPLE이라 무시
      a.incrementStrongPickCount();

      BattleHotList hot = BattleHotList.builder().id(22L).voteType(VoteType.MULTIPLE).build();
      given(battleRepository.findHotBattlesWithSearchCondition(pageable))
          .willReturn(new PageImpl<>(List.of(hot), pageable, 1));
      given(battleItemRepository.findByBattleIdInAndStatusOrderByTotalScoreDesc(
          List.of(22L), BattleItemStatus.ACTIVE))
          .willReturn(List.of(a, b));

      // when
      Page<BattleHotList> result = battleService.getHotBattleList(pageable);

      // then - vote totalScore 기준
      List<BattleHotItem> topItems = result.getContent().get(0).getTopItems();
      assertThat(topItems.get(0).getId()).isEqualTo(100L);
      assertThat(topItems.get(0).getTotalScore()).isEqualTo(7);
      // votePercentage 분모는 active 아이템 totalScore 합 (7+3 = 10)
      assertThat(topItems.get(0).getVotePercentage()).isEqualTo(7.0 / 10.0);
    }

    @Test
    @DisplayName("SWIPE/vote 배틀 혼합 페이지: 각 배틀의 voteType에 맞게 정렬·매핑된다")
    void 혼합_페이지() {
      // given
      Battle swipe = swipeBattle(21L);
      Battle vote = voteBattle(22L);
      BattleItem swipeA = swipeItem(swipe, 100L, "swipeA", 0, 1, 0);  // 1
      BattleItem swipeB = swipeItem(swipe, 101L, "swipeB", 1, 0, 0);  // 3
      BattleItem voteA = voteItem(vote, 200L, "voteA", 5);
      BattleItem voteB = voteItem(vote, 201L, "voteB", 2);

      BattleHotList hotSwipe = BattleHotList.builder().id(21L).voteType(VoteType.SWIPE).build();
      BattleHotList hotVote = BattleHotList.builder().id(22L).voteType(VoteType.MULTIPLE).build();
      given(battleRepository.findHotBattlesWithSearchCondition(pageable))
          .willReturn(new PageImpl<>(List.of(hotSwipe, hotVote), pageable, 2));
      given(battleItemRepository.findByBattleIdInAndStatusOrderByTotalScoreDesc(
          List.of(21L, 22L), BattleItemStatus.ACTIVE))
          .willReturn(List.of(swipeA, swipeB, voteA, voteB));

      // when
      Page<BattleHotList> result = battleService.getHotBattleList(pageable);

      // then
      List<BattleHotItem> swipeTop = result.getContent().get(0).getTopItems();
      assertThat(swipeTop.get(0).getId()).isEqualTo(101L);  // swipeB = 3
      assertThat(swipeTop.get(0).getTotalScore()).isEqualTo(3);

      List<BattleHotItem> voteTop = result.getContent().get(1).getTopItems();
      assertThat(voteTop.get(0).getId()).isEqualTo(200L);  // voteA = 5
      assertThat(voteTop.get(0).getTotalScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("빈 페이지면 아이템 fetch 쿼리를 호출하지 않는다")
    void 빈_페이지() {
      given(battleRepository.findHotBattlesWithSearchCondition(pageable))
          .willReturn(new PageImpl<>(List.of(), pageable, 0));

      Page<BattleHotList> result = battleService.getHotBattleList(pageable);

      assertThat(result.getContent()).isEmpty();
      then(battleItemRepository).shouldHaveNoInteractions();
    }

    private Battle swipeBattle(Long id) {
      return buildBattle(id, VoteType.SWIPE);
    }

    private Battle voteBattle(Long id) {
      return buildBattle(id, VoteType.MULTIPLE);
    }

    private Battle buildBattle(Long id, VoteType voteType) {
      LocalDateTime now = LocalDateTime.now();
      return Battle.builder()
          .id(id)
          .title("배틀 " + id)
          .creatorId(CREATOR_ID)
          .startDate(now.minusDays(1))
          .participationStartDate(now.minusDays(1))
          .endDate(now.plusDays(7))
          .itemAddPermissionType(ItemAddPermissionType.PUBLIC_FREE)
          .voteType(voteType)
          .status(BattleStatus.NORMAL)
          .build();
    }

    private BattleItem swipeItem(Battle battle, Long id, String name, int strong, int pick,
        int pass) {
      return BattleItem.builder()
          .id(id)
          .battle(battle)
          .itemType(BattleItemType.CUSTOM)
          .customName(name)
          .registerId(CREATOR_ID)
          .status(BattleItemStatus.ACTIVE)
          .strongPickCount(strong)
          .pickCount(pick)
          .passCount(pass)
          .build();
    }

    private BattleItem voteItem(Battle battle, Long id, String name, int totalScore) {
      return BattleItem.builder()
          .id(id)
          .battle(battle)
          .itemType(BattleItemType.CUSTOM)
          .customName(name)
          .registerId(CREATOR_ID)
          .status(BattleItemStatus.ACTIVE)
          .totalScore(totalScore)
          .build();
    }
  }
}
