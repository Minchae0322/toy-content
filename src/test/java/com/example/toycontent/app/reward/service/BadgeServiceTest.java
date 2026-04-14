package com.example.toycontent.app.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.reward.controller.dto.RewardRequest;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.BadgeInfo;
import com.example.toycontent.app.reward.domain.Badge;
import com.example.toycontent.app.reward.repository.BadgeRepository;
import com.example.toycontent.support.fixture.BadgeFixture;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeService")
class BadgeServiceTest {

  @Mock private BadgeRepository badgeRepository;
  @InjectMocks private BadgeService badgeService;

  @Nested
  @DisplayName("createBadge - 뱃지 생성")
  class CreateBadge {

    @Test
    @DisplayName("코드가 중복되지 않으면 뱃지를 정상 생성한다")
    void 정상_생성() {
      // given
      RewardRequest.CreateBadge request = RewardRequest.CreateBadge.builder()
          .code("NEW_BADGE")
          .name("새 뱃지")
          .description("설명")
          .category("BRAG")
          .build();
      given(badgeRepository.existsByCode("NEW_BADGE")).willReturn(false);
      given(badgeRepository.save(any(Badge.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      BadgeInfo result = badgeService.createBadge(request);

      // then
      assertThat(result.getCode()).isEqualTo("NEW_BADGE");
      then(badgeRepository).should().save(any(Badge.class));
    }

    @Test
    @DisplayName("코드가 중복되면 RestApiException을 던진다")
    void 코드_중복_예외() {
      // given
      RewardRequest.CreateBadge request = RewardRequest.CreateBadge.builder()
          .code("EXISTING")
          .name("뱃지")
          .build();
      given(badgeRepository.existsByCode("EXISTING")).willReturn(true);

      // when & then
      assertThatThrownBy(() -> badgeService.createBadge(request))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getBadgeById - 뱃지 조회")
  class GetBadgeById {

    @Test
    @DisplayName("존재하는 뱃지를 정상 조회한다")
    void 정상_조회() {
      // given
      Badge badge = BadgeFixture.basic();
      given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));

      // when
      Badge result = badgeService.getBadgeById(1L);

      // then
      assertThat(result.getCode()).isEqualTo(BadgeFixture.DEFAULT_CODE);
    }

    @Test
    @DisplayName("존재하지 않는 뱃지 조회 시 RestApiException을 던진다")
    void 조회_실패() {
      // given
      given(badgeRepository.findById(999L)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> badgeService.getBadgeById(999L))
          .isInstanceOf(RestApiException.class);
    }
  }

  @Nested
  @DisplayName("getBadgeByCode - 뱃지 코드 조회")
  class GetBadgeByCode {

    @Test
    @DisplayName("존재하지 않는 코드 조회 시 RestApiException을 던진다")
    void 코드_조회_실패() {
      // given
      given(badgeRepository.findByCode("INVALID")).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> badgeService.getBadgeByCode("INVALID"))
          .isInstanceOf(RestApiException.class);
    }
  }
}
