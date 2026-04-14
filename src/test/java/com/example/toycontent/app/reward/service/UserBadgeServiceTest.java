package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserBadgeInfo;
import com.example.toycontent.app.reward.domain.Badge;
import com.example.toycontent.app.reward.domain.UserBadge;
import com.example.toycontent.app.reward.repository.UserBadgeRepository;
import com.example.toycontent.support.fixture.BadgeFixture;
import com.example.toycontent.support.fixture.UserBadgeFixture;
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
@DisplayName("UserBadgeService")
class UserBadgeServiceTest {

  private static final Long USER_ID = 100L;

  @Mock private UserBadgeRepository userBadgeRepository;
  @Mock private BadgeService badgeService;
  @InjectMocks private UserBadgeService userBadgeService;

  @Nested
  @DisplayName("awardBadge - 뱃지 수여")
  class AwardBadge {

    @Test
    @DisplayName("미획득 뱃지를 정상 수여한다")
    void 정상_수여() {
      // given
      Badge badge = BadgeFixture.basic();
      given(badgeService.getBadgeByCode("BUY_PLACE_SHARER")).willReturn(badge);
      given(userBadgeRepository.existsByUserIdAndBadgeIdAndRevokedFalse(USER_ID, badge.getId()))
          .willReturn(false);
      given(userBadgeRepository.save(any(UserBadge.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      UserBadge result = userBadgeService.awardBadge(USER_ID, "BUY_PLACE_SHARER");

      // then
      assertThat(result.getUserId()).isEqualTo(USER_ID);
      then(userBadgeRepository).should().save(any(UserBadge.class));
    }

    @Test
    @DisplayName("이미 획득한 뱃지면 RestApiException을 던진다")
    void 중복_수여_예외() {
      // given
      Badge badge = BadgeFixture.basic();
      given(badgeService.getBadgeByCode("BUY_PLACE_SHARER")).willReturn(badge);
      given(userBadgeRepository.existsByUserIdAndBadgeIdAndRevokedFalse(USER_ID, badge.getId()))
          .willReturn(true);

      // when & then
      assertThatThrownBy(() -> userBadgeService.awardBadge(USER_ID, "BUY_PLACE_SHARER"))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("awardBadgeIfAbsent - 뱃지 조건부 수여")
  class AwardBadgeIfAbsent {

    @Test
    @DisplayName("이미 획득한 뱃지면 null을 반환한다")
    void 이미_획득시_null() {
      // given
      Badge badge = BadgeFixture.basic();
      given(badgeService.getBadgeByCode("BUY_PLACE_SHARER")).willReturn(badge);
      given(userBadgeRepository.existsByUserIdAndBadgeIdAndRevokedFalse(USER_ID, badge.getId()))
          .willReturn(true);

      // when
      UserBadge result = userBadgeService.awardBadgeIfAbsent(USER_ID, "BUY_PLACE_SHARER");

      // then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("revokeBadge - 뱃지 회수")
  class RevokeBadge {

    @Test
    @DisplayName("유효한 뱃지를 정상 회수한다")
    void 정상_회수() {
      // given
      UserBadge userBadge = UserBadgeFixture.basic();
      given(userBadgeRepository.findByUserIdAndBadgeId(USER_ID, 1L))
          .willReturn(Optional.of(userBadge));

      // when
      userBadgeService.revokeBadge(USER_ID, 1L, "어뷰징");

      // then
      assertThat(userBadge.getRevoked()).isTrue();
    }

    @Test
    @DisplayName("이미 회수된 뱃지면 RestApiException을 던진다")
    void 이미_회수_예외() {
      // given
      UserBadge userBadge = UserBadgeFixture.revoked();
      given(userBadgeRepository.findByUserIdAndBadgeId(USER_ID, 1L))
          .willReturn(Optional.of(userBadge));

      // when & then
      assertThatThrownBy(() -> userBadgeService.revokeBadge(USER_ID, 1L, "어뷰징"))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getUserBadges - 유저 뱃지 목록 조회")
  class GetUserBadges {

    @Test
    @DisplayName("QueryDSL을 통해 유저 뱃지 목록을 조회한다")
    void 정상_조회() {
      // given
      UserBadgeInfo badgeInfo = UserBadgeInfo.builder()
          .id(1L)
          .pinned(false)
          .build();
      given(userBadgeRepository.findUserBadgesWithBadgeDetail(USER_ID))
          .willReturn(List.of(badgeInfo));

      // when
      List<UserBadgeInfo> result = userBadgeService.getUserBadges(USER_ID);

      // then
      assertThat(result).hasSize(1);
    }
  }
}
