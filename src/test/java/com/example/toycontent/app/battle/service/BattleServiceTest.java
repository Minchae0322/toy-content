package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.toycontent.app.battle.controller.dto.BattleRequest.BattleItemsSearchCondition;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleAttachmentFileRepository;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import com.example.toycontent.support.fixture.BattleFixture;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @InjectMocks private BattleService battleService;

  @Nested
  @DisplayName("validateCreation - 배틀 생성 제한 검증")
  class ValidateCreation {

    @Test
    @DisplayName("현재 활성 배틀이 10개 미만이고 24시간 내 생성이 3개 미만이면 검증을 통과한다")
    void 정상_생성_가능() {
      // given
      given(battleRepository.countByCreatorIdAndStatus(CREATOR_ID, BattleStatus.NORMAL))
          .willReturn(5L);
      given(battleRepository.countByCreatorIdAndCreatedAtAfter(eq(CREATOR_ID), any()))
          .willReturn(1L);

      // expect - 예외 없이 실행
      battleService.validateCreation(CREATOR_ID);
    }

    @Test
    @DisplayName("현재 활성 배틀이 10개 이상이면 MAX_ACTIVE_BATTLES 예외를 던진다")
    void 활성_배틀_초과_예외() {
      // given
      given(battleRepository.countByCreatorIdAndStatus(CREATOR_ID, BattleStatus.NORMAL))
          .willReturn(10L);

      // when & then
      assertThatThrownBy(() -> battleService.validateCreation(CREATOR_ID))
          .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("24시간 내 생성이 3개 이상이면 DAILY_LIMIT 예외를 던진다")
    void 일일_생성_초과_예외() {
      // given
      given(battleRepository.countByCreatorIdAndStatus(CREATOR_ID, BattleStatus.NORMAL))
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
}
