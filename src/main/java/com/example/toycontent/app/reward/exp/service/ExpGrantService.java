package com.example.toycontent.app.reward.exp.service;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.common.enumuration.StreakMilestone;
import com.example.toycontent.app.reward.exp.config.RewardProperties;
import com.example.toycontent.app.reward.exp.domain.DailyExpCap;
import com.example.toycontent.app.reward.exp.repository.DailyExpCapRepository;
import com.example.toycontent.app.reward.exp.repository.ExpHistoryRepository;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EXP 지급 오케스트레이터.
 *
 * <p>각 유저 행동(피드 작성, 리액션, 배틀 투표 등)에 대한 EXP 지급을 담당한다.
 * 일일 캡(200 EXP)과 중복 지급 방지를 내부적으로 처리한다.</p>
 *
 * <p>지급 금액은 {@link ExpSource#getDefaultAmount()}에, 캡 제외 여부는
 * {@link ExpSource#isCapExempt()}에 정의되어 있다. 스트릭 마일스톤은
 * {@link StreakMilestone}을 참조한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpGrantService {

  private final UserRewardService userRewardService;
  private final ExpHistoryRepository expHistoryRepository;
  private final DailyExpCapRepository dailyExpCapRepository;
  private final RewardProperties rewardProperties;

  private static final long BATTLE_ITEM_ADD_MAX_PER_BATTLE = 5L;

  // ── 피드 ──

  @Transactional
  public ExpGrantResult grantFeedCreate(Long userId, Long feedId) {
    return grant(userId, ExpSource.FEED_CREATE, feedId);
  }

  /**
   * 피드 완성도 보너스 지급.
   * qualityScore: 3→10, 4→20, 5→30
   */
  @Transactional
  public ExpGrantResult grantFeedQualityBonus(Long userId, Long feedId, int qualityScore) {
    if (qualityScore < 3) {
      return ExpGrantResult.cappedOut(0);
    }
    long bonusExp = (qualityScore - 2) * 10L;
    return grantWithCap(userId, bonusExp, ExpSource.FEED_CREATE, feedId);
  }

  @Transactional
  public ExpGrantResult grantFeedReaction(Long feedOwnerId, Long feedId) {
    return grant(feedOwnerId, ExpSource.FEED_REACTION, feedId);
  }

  // ── 댓글 ──

  @Transactional
  public ExpGrantResult grantCommentCreate(Long userId, Long commentId) {
    return grant(userId, ExpSource.COMMENT_CREATE, commentId);
  }

  // ── 배틀 ──

  @Transactional
  public ExpGrantResult grantBattleVote(Long userId, Long battleId) {
    return grant(userId, ExpSource.BATTLE_VOTE, battleId);
  }

  @Transactional
  public ExpGrantResult grantBattleItemAdd(Long userId, Long battleId) {
    return grantWithCountCap(userId, ExpSource.BATTLE_ITEM_ADD, battleId, BATTLE_ITEM_ADD_MAX_PER_BATTLE);
  }

  @Transactional
  public ExpGrantResult grantBattleWeightedVote(Long userId, Long battleId) {
    return grant(userId, ExpSource.BATTLE_WEIGHTED_VOTE, battleId);
  }

  @Transactional
  public ExpGrantResult grantBattlePredictionHit(Long userId, Long predictionId) {
    return grant(userId, ExpSource.BATTLE_PREDICTION_HIT, predictionId);
  }

  // ── PICK ──

  @Transactional
  public ExpGrantResult grantPickComment(Long userId, Long pickCommentId) {
    return grant(userId, ExpSource.PICK_COMMENT, pickCommentId);
  }

  // ── 발굴 ──

  @Transactional
  public ExpGrantResult grantHotDiscover(Long userId, Long feedId) {
    return grant(userId, ExpSource.HOT_DISCOVER, feedId);
  }

  // ── 출석 ──

  @Transactional
  public ExpGrantResult grantAttendance(Long userId) {
    return grant(userId, ExpSource.ATTENDANCE, null);
  }

  // ── 스트릭 보너스 (캡 제외) ──

  @Transactional
  public ExpGrantResult grantStreakBonus(Long userId, int days) {
    return StreakMilestone.from(days)
        .map(m -> grantWithoutCap(userId, m.getBonusExp(), ExpSource.STREAK_BONUS, (long) days))
        .orElseGet(() -> ExpGrantResult.cappedOut(0));
  }

  // ── 미션 보상 (캡 제외) ──

  @Transactional
  public ExpGrantResult grantMissionClaim(Long userId, Long assignmentId, int rewardExp) {
    return grantWithoutCap(userId, rewardExp, ExpSource.MISSION_CLAIM, assignmentId);
  }

  // ── 내부 메서드 ──

  /**
   * ExpSource의 정책(defaultAmount, capExempt)을 따라 지급한다.
   */
  private ExpGrantResult grant(Long userId, ExpSource source, Long sourceId) {
    long amount = source.getDefaultAmount();

    return source.isCapExempt()
        ? grantWithoutCap(userId, amount, source, sourceId)
        : grantWithCap(userId, amount, source, sourceId);
  }

  /**
   * 일일 캡을 적용하여 EXP를 지급한다. 중복/캡소진이면 실제 지급은 건너뛴다.
   */
  private ExpGrantResult grantWithCap(Long userId, long amount, ExpSource source, Long sourceId) {
    // 같은 게시물로 이미 지급됐으면 스킵
    if (sourceId != null && isDuplicate(userId, source, sourceId)) {
      return ExpGrantResult.duplicated(amount);
    }

    // 오늘 남은 캡만큼만 차감 (요청량보다 적으면 부분 지급)
    DailyExpCap cap = getOrCreateDailyExpCap(userId);
    long actualAmount = cap.consume(amount, rewardProperties.dailyExpCap());

    if (actualAmount <= 0) {
      return ExpGrantResult.cappedOut(amount);
    }

    userRewardService.addExp(userId, actualAmount, source, sourceId);
    boolean capped = actualAmount < amount;

    log.info("EXP 지급 - userId: {}, source: {}, requested: {}, actual: {}, capped: {}",
        userId, source, amount, actualAmount, capped);

    return ExpGrantResult.granted(amount, actualAmount, capped);
  }

  /**
   * sourceId별 최대 지급 횟수를 카운트 기반으로 제한하며 지급한다.
   * 일일 캡은 그대로 적용된다.
   */
  private ExpGrantResult grantWithCountCap(Long userId, ExpSource source, Long sourceId, long maxCount) {
    long amount = source.getDefaultAmount();

    if (sourceId != null
        && expHistoryRepository.countByUserIdAndSourceAndSourceId(userId, source, sourceId) >= maxCount) {
      return ExpGrantResult.duplicated(amount);
    }

    DailyExpCap cap = getOrCreateDailyExpCap(userId);
    long actualAmount = cap.consume(amount, rewardProperties.dailyExpCap());

    if (actualAmount <= 0) {
      return ExpGrantResult.cappedOut(amount);
    }

    userRewardService.addExp(userId, actualAmount, source, sourceId);
    boolean capped = actualAmount < amount;

    log.info("EXP 지급 (횟수 제한) - userId: {}, source: {}, sourceId: {}, requested: {}, actual: {}, capped: {}",
        userId, source, sourceId, amount, actualAmount, capped);

    return ExpGrantResult.granted(amount, actualAmount, capped);
  }

  /**
   * 캡 없이 EXP를 지급한다 (미션/스트릭 보너스용).
   */
  private ExpGrantResult grantWithoutCap(Long userId, long amount, ExpSource source, Long sourceId) {
    if (sourceId != null && isDuplicate(userId, source, sourceId)) {
      log.debug("EXP 중복 지급 방지 - userId: {}, source: {}, sourceId: {}", userId, source, sourceId);
      return ExpGrantResult.duplicated(amount);
    }

    userRewardService.addExp(userId, amount, source, sourceId);

    log.info("EXP 지급 (캡 제외) - userId: {}, source: {}, amount: {}", userId, source, amount);

    return ExpGrantResult.granted(amount, amount, false);
  }

  private boolean isDuplicate(Long userId, ExpSource source, Long sourceId) {
    return expHistoryRepository.existsByUserIdAndSourceAndSourceId(userId, source, sourceId);
  }

  private DailyExpCap getOrCreateDailyExpCap(Long userId) {
    LocalDate today = LocalDate.now(rewardProperties.timeZone());
    return dailyExpCapRepository.findByUserIdAndCapDate(userId, today)
        .orElseGet(() -> dailyExpCapRepository.save(
            DailyExpCap.builder()
                .userId(userId)
                .capDate(today)
                .build()
        ));
  }
}
