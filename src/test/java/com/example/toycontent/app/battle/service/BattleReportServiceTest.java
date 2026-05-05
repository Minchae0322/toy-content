package com.example.toycontent.app.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.toycontent.app.battle.controller.dto.BattleReportRequest;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleReport;
import com.example.toycontent.app.battle.repository.BattleReportRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.common.enumuration.ReportReason;
import com.example.toycontent.app.common.enumuration.ReportStatus;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.support.fixture.BattleFixture;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleReportService")
class BattleReportServiceTest {

  private static final long BATTLE_ID = BattleFixture.DEFAULT_BATTLE_ID;
  private static final long CREATOR_ID = BattleFixture.DEFAULT_CREATOR_ID;
  private static final long REPORTER_ID = 200L;
  private static final long REPORT_ID = 999L;

  @Mock private BattleRepository battleRepository;
  @Mock private BattleReportRepository battleReportRepository;

  @InjectMocks private BattleReportService battleReportService;

  private BattleReportRequest request(ReportReason reason, String detail) {
    return new BattleReportRequest(reason, detail);
  }

  @Nested
  @DisplayName("report")
  class Report {

    @Test
    @DisplayName("정상 신고 시 BattleReport가 저장되고 PENDING 상태로 생성된다")
    void 정상_신고() throws Exception {
      // given
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleReportRepository.existsByBattleIdAndReporterId(BATTLE_ID, REPORTER_ID))
          .willReturn(false);
      given(battleReportRepository.save(any(BattleReport.class)))
          .willAnswer(invocation -> {
            BattleReport saved = invocation.getArgument(0);
            setId(saved, REPORT_ID);
            return saved;
          });

      // when
      Long savedId = battleReportService.report(BATTLE_ID, REPORTER_ID,
          request(ReportReason.SEXUAL, "음란"));

      // then
      ArgumentCaptor<BattleReport> captor = ArgumentCaptor.forClass(BattleReport.class);
      then(battleReportRepository).should().save(captor.capture());
      BattleReport saved = captor.getValue();

      assertSoftly(softly -> {
        softly.assertThat(savedId).isEqualTo(REPORT_ID);
        softly.assertThat(saved.getBattle()).isSameAs(battle);
        softly.assertThat(saved.getReporterId()).isEqualTo(REPORTER_ID);
        softly.assertThat(saved.getReason()).isEqualTo(ReportReason.SEXUAL);
        softly.assertThat(saved.getDetail()).isEqualTo("음란");
        softly.assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
      });
    }

    @Test
    @DisplayName("detail은 null이어도 신고가 성립한다")
    void detail_없이_신고() throws Exception {
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleReportRepository.existsByBattleIdAndReporterId(BATTLE_ID, REPORTER_ID))
          .willReturn(false);
      given(battleReportRepository.save(any(BattleReport.class)))
          .willAnswer(invocation -> {
            BattleReport saved = invocation.getArgument(0);
            setId(saved, REPORT_ID);
            return saved;
          });

      Long savedId = battleReportService.report(BATTLE_ID, REPORTER_ID,
          request(ReportReason.OFFENSIVE, null));

      assertThat(savedId).isEqualTo(REPORT_ID);
    }

    @Test
    @DisplayName("배틀이 존재하지 않으면 BATTLE_NOT_FOUND 예외가 발생한다")
    void 배틀_없음() {
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.empty());

      assertThatThrownBy(() -> battleReportService.report(BATTLE_ID, REPORTER_ID,
          request(ReportReason.SPAM, null)))
          .isInstanceOf(RestApiException.class)
          .hasFieldOrPropertyWithValue("errorCode", BattleErrorCode.BATTLE_NOT_FOUND);

      then(battleReportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("본인이 생성한 배틀은 신고할 수 없다 (BATTLE_REPORT_SELF)")
    void 본인_배틀_신고_불가() {
      Battle battle = BattleFixture.active(); // creator = CREATOR_ID
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));

      // reporter == creator
      assertThatThrownBy(() -> battleReportService.report(BATTLE_ID, CREATOR_ID,
          request(ReportReason.SPAM, null)))
          .isInstanceOf(RestApiException.class)
          .hasFieldOrPropertyWithValue("errorCode", BattleErrorCode.BATTLE_REPORT_SELF);

      then(battleReportRepository).should(never())
          .existsByBattleIdAndReporterId(any(), any());
      then(battleReportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 신고한 배틀을 다시 신고하면 BATTLE_REPORT_DUPLICATED 예외가 발생한다")
    void 중복_신고_불가() {
      Battle battle = BattleFixture.active();
      given(battleRepository.findById(BATTLE_ID)).willReturn(Optional.of(battle));
      given(battleReportRepository.existsByBattleIdAndReporterId(BATTLE_ID, REPORTER_ID))
          .willReturn(true);

      assertThatThrownBy(() -> battleReportService.report(BATTLE_ID, REPORTER_ID,
          request(ReportReason.SPAM, null)))
          .isInstanceOf(RestApiException.class)
          .hasFieldOrPropertyWithValue("errorCode", BattleErrorCode.BATTLE_REPORT_DUPLICATED);

      then(battleReportRepository).should(never()).save(any());
    }
  }

  /**
   * 테스트에서 save 시점의 ID 부여를 흉내내기 위해 BaseReport.id를 리플렉션으로 설정.
   */
  private static void setId(BattleReport report, Long id) throws Exception {
    Field idField = report.getClass().getSuperclass().getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(report, id);
  }
}
