package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.reward.domain.DailyExpCap;
import com.example.toycontent.app.reward.repository.DailyExpCapRepository;
import com.example.toycontent.app.reward.repository.ExpHistoryRepository;
import com.example.toycontent.app.reward.service.dto.ExpGrantResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
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
 * <p>미션 보상(MISSION_CLAIM)과 스트릭 보너스(STREAK_BONUS)는 캡에서 제외된다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpGrantService {

  private static final long DAILY_CAP = 200L;
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static final int FEED_CREATE_EXP = 20;
  private static final int FEED_REACTION_EXP = 5;
  private static final int COMMENT_CREATE_EXP = 5;
  private static final int BATTLE_VOTE_EXP = 5;
  private static final int BATTLE_WEIGHTED_VOTE_EXP = 10;
  private static final int BATTLE_PREDICTION_HIT_EXP = 30;
  private static final int PICK_COMMENT_EXP = 10;
  private static final int HOT_DISCOVER_EXP = 50;
  private static final int ATTENDANCE_EXP = 5;

  /** 스트릭 마일스톤별 보너스 EXP */
  private static final Map<Integer, Integer> STREAK_MILESTONES = Map.of(
      3, 20,
      7, 50,
      14, 100,
      30, 200,
      100, 500
  );

  private final UserRewardService userRewardService;
  private final ExpHistoryRepository expHistoryRepository;
  private final DailyExpCapRepository dailyExpCapRepository;

  // ── 피드 ──

  @Transactional
  public ExpGrantResult grantFeedCreate(Long userId, Long feedId) {
    return grantWithCap(userId, FEED_CREATE_EXP, ExpSource.FEED_CREATE, feedId);
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
    int bonusExp = (qualityScore - 2) * 10;
    return grantWithCap(userId, bonusExp, ExpSource.FEED_CREATE, feedId);
  }

  @Transactional
  public ExpGrantResult grantFeedReaction(Long feedOwnerId, Long feedId) {
    return grantWithCap(feedOwnerId, FEED_REACTION_EXP, ExpSource.FEED_REACTION, feedId);
  }

  // ── 댓글 ──

  @Transactional
  public ExpGrantResult grantCommentCreate(Long userId, Long commentId) {
    return grantWithCap(userId, COMMENT_CREATE_EXP, ExpSource.COMMENT_CREATE, commentId);
  }

  // ── 배틀 ──

  @Transactional
  public ExpGrantResult grantBattleVote(Long userId, Long battleId) {
    return grantWithCap(userId, BATTLE_VOTE_EXP, ExpSource.BATTLE_VOTE, battleId);
  }

  @Transactional
  public ExpGrantResult grantBattleWeightedVote(Long userId, Long battleId) {
    return grantWithCap(userId, BATTLE_WEIGHTED_VOTE_EXP, ExpSource.BATTLE_WEIGHTED_VOTE, battleId);
  }

  @Transactional
  public ExpGrantResult grantBattlePredictionHit(Long userId, Long predictionId) {
    return grantWithCap(userId, BATTLE_PREDICTION_HIT_EXP, ExpSource.BATTLE_PREDICTION_HIT, predictionId);
  }

  // ── PICK ──

  @Transactional
  public ExpGrantResult grantPickComment(Long userId, Long pickCommentId) {
    return grantWithCap(userId, PICK_COMMENT_EXP, ExpSource.PICK_COMMENT, pickCommentId);
  }

  // ── 발굴 ──

  @Transactional
  public ExpGrantResult grantHotDiscover(Long userId, Long feedId) {
    return grantWithCap(userId, HOT_DISCOVER_EXP, ExpSource.HOT_DISCOVER, feedId);
  }

  // ── 출석 ──

  @Transactional
  public ExpGrantResult grantAttendance(Long userId) {
    return grantWithCap(userId, ATTENDANCE_EXP, ExpSource.ATTENDANCE, null);
  }

  // ── 스트릭 보너스 (캡 제외) ──

  @Transactional
  public ExpGrantResult grantStreakBonus(Long userId, int milestone) {
    Integer bonusExp = STREAK_MILESTONES.get(milestone);
    if (bonusExp == null) {
      return ExpGrantResult.cappedOut(0);
    }
    return grantWithoutCap(userId, bonusExp, ExpSource.STREAK_BONUS, (long) milestone);
  }

  // ── 미션 보상 (캡 제외) ──

  @Transactional
  public ExpGrantResult grantMissionClaim(Long userId, Long assignmentId, int rewardExp) {
    return grantWithoutCap(userId, rewardExp, ExpSource.MISSION_CLAIM, assignmentId);
  }

  // ── 내부 메서드 ──

  /**
   * 일일 캡을 적용하여 EXP를 지급한다.
   * 중복 지급 방지: 같은 (userId, source, sourceId) 조합이면 건너뛴다.
   */
  private ExpGrantResult grantWithCap(Long userId, long amount, ExpSource source, Long sourceId) {
    if (sourceId != null && isDuplicate(userId, source, sourceId)) {
      log.debug("EXP 중복 지급 방지 - userId: {}, source: {}, sourceId: {}", userId, source, sourceId);
      return ExpGrantResult.duplicated(amount);
    }

    DailyExpCap cap = getOrCreateDailyExpCap(userId);
    long actualAmount = cap.consume(amount, DAILY_CAP);

    if (actualAmount <= 0) {
      log.debug("일일 EXP 캡 초과 - userId: {}, source: {}", userId, source);
      return ExpGrantResult.cappedOut(amount);
    }

    userRewardService.addExp(userId, actualAmount, source, sourceId);
    boolean capped = actualAmount < amount;

    log.info("EXP 지급 - userId: {}, source: {}, requested: {}, actual: {}, capped: {}",
        userId, source, amount, actualAmount, capped);

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
    LocalDate today = LocalDate.now(KST);
    return dailyExpCapRepository.findByUserIdAndCapDate(userId, today)
        .orElseGet(() -> dailyExpCapRepository.save(
            DailyExpCap.builder()
                .userId(userId)
                .capDate(today)
                .build()
        ));
  }
}
