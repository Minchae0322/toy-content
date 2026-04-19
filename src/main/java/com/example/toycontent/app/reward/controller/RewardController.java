package com.example.toycontent.app.reward.controller;

import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.reward.controller.dto.RewardRequest;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.CategoryMasteryInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.MissionAssignmentInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.PredictionInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.PredictionStats;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserBadgeInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserRewardInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserStreakInfo;
import com.example.toycontent.app.reward.domain.UserReward;
import com.example.toycontent.app.reward.service.BattlePredictionService;
import com.example.toycontent.app.reward.service.CategoryMasteryService;
import com.example.toycontent.app.reward.service.LevelExpService;
import com.example.toycontent.app.reward.service.UserBadgeService;
import com.example.toycontent.app.reward.service.UserDailyMissionAssignmentService;
import com.example.toycontent.app.reward.service.UserRewardService;
import com.example.toycontent.app.reward.service.UserStreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RewardController", description = "보상 API")
@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
public class RewardController {

  private final UserRewardService userRewardService;
  private final LevelExpService levelExpService;
  private final UserBadgeService userBadgeService;
  private final UserStreakService userStreakService;
  private final UserDailyMissionAssignmentService missionAssignmentService;
  private final BattlePredictionService battlePredictionService;
  private final CategoryMasteryService categoryMasteryService;

  @Operation(summary = "내 EXP/레벨 조회")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserRewardInfo>> getMyReward(
      @CurrentUserId Long userId) {
    UserReward reward = userRewardService.getOrCreateUserReward(userId);
    return ResponseEntity.ok(
        ApiResponse.success(UserRewardInfo.of(reward, levelExpService.computeLevelInfo(reward.getTotalExp()))));
  }

  @Operation(summary = "내 뱃지 목록 조회")
  @GetMapping("/me/badges")
  public ResponseEntity<ApiResponse<List<UserBadgeInfo>>> getMyBadges(
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(ApiResponse.success(userBadgeService.getUserBadges(userId)));
  }

  @Operation(summary = "뱃지 프로필 고정")
  @PostMapping("/me/badges/{userBadgeId}/pin")
  public ResponseEntity<ApiResponse<Void>> pinBadge(
      @CurrentUserId Long userId,
      @Parameter(description = "유저 뱃지 ID") @PathVariable Long userBadgeId) {
    userBadgeService.pinBadge(userId, userBadgeId);
    return ResponseEntity.ok(ApiResponse.success(null, "뱃지가 고정되었습니다."));
  }

  @Operation(summary = "뱃지 프로필 고정 해제")
  @DeleteMapping("/me/badges/{userBadgeId}/pin")
  public ResponseEntity<ApiResponse<Void>> unpinBadge(
      @CurrentUserId Long userId,
      @Parameter(description = "유저 뱃지 ID") @PathVariable Long userBadgeId) {
    userBadgeService.unpinBadge(userId, userBadgeId);
    return ResponseEntity.ok(ApiResponse.success(null, "뱃지 고정이 해제되었습니다."));
  }

  @Operation(summary = "내 스트릭 조회")
  @GetMapping("/me/streak")
  public ResponseEntity<ApiResponse<UserStreakInfo>> getMyStreak(
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(
        ApiResponse.success(UserStreakInfo.from(userStreakService.getOrCreateUserStreak(userId))));
  }

  @Operation(summary = "오늘 작성 인증")
  @PostMapping("/me/streak/posting")
  public ResponseEntity<ApiResponse<UserStreakInfo>> recordPosting(
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(
        ApiResponse.success(UserStreakInfo.from(userStreakService.recordPosting(userId)), "인증되었습니다."));
  }

  @Operation(summary = "스트릭 복구 티켓 사용")
  @PostMapping("/me/streak/recovery")
  public ResponseEntity<ApiResponse<UserStreakInfo>> useRecoveryTicket(
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(
        ApiResponse.success(UserStreakInfo.from(userStreakService.useRecoveryTicket(userId)), "복구 티켓을 사용했습니다."));
  }

  @Operation(summary = "오늘 미션 조회")
  @GetMapping("/me/missions/today")
  public ResponseEntity<ApiResponse<List<MissionAssignmentInfo>>> getTodayMissions(
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(ApiResponse.success(missionAssignmentService.getTodayAssignments(userId)));
  }

  @Operation(summary = "날짜별 미션 조회")
  @GetMapping("/me/missions")
  public ResponseEntity<ApiResponse<List<MissionAssignmentInfo>>> getMissionsByDate(
      @CurrentUserId Long userId,
      @Parameter(description = "조회 일자 (yyyy-MM-dd)")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(ApiResponse.success(missionAssignmentService.getAssignmentsByDate(userId, date)));
  }

  @Operation(summary = "일일 미션 배정")
  @PostMapping("/me/missions/assign")
  public ResponseEntity<ApiResponse<List<MissionAssignmentInfo>>> assignMissions(
      @CurrentUserId Long userId,
      @Valid @RequestBody RewardRequest.AssignMissions request) {
    List<MissionAssignmentInfo> assignments = missionAssignmentService
        .assignDailyMissions(userId, LocalDate.now(), request.getMissionIds())
        .stream()
        .map(MissionAssignmentInfo::from)
        .toList();
    return ResponseEntity.ok(ApiResponse.success(assignments, "미션이 배정되었습니다."));
  }

  @Operation(summary = "미션 보상 수령")
  @PostMapping("/me/missions/{assignmentId}/claim")
  public ResponseEntity<ApiResponse<MissionAssignmentInfo>> claimMissionReward(
      @CurrentUserId Long userId,
      @Parameter(description = "미션 배정 ID") @PathVariable Long assignmentId) {
    return ResponseEntity.ok(
        ApiResponse.success(MissionAssignmentInfo.from(missionAssignmentService.claimReward(userId, assignmentId)), "보상을 수령했습니다."));
  }

  // ── 배틀 예측 ──

  @Operation(summary = "내 예측 이력 조회")
  @GetMapping("/me/predictions")
  public ResponseEntity<ApiResponse<List<PredictionInfo>>> getMyPredictions(
      @CurrentUserId Long userId,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.success(battlePredictionService.getUserPredictionHistory(userId, pageable)));
  }

  @Operation(summary = "내 예측 통계 조회")
  @GetMapping("/me/predictions/stats")
  public ResponseEntity<ApiResponse<PredictionStats>> getMyPredictionStats(
      @CurrentUserId Long userId) {
    long hitCount = battlePredictionService.getUserHitCount(userId);
    long totalCount = battlePredictionService.getUserTotalPredictions(userId);
    return ResponseEntity.ok(ApiResponse.success(PredictionStats.of(hitCount, totalCount)));
  }

  // ── 카테고리 숙련도 ──

  @Operation(summary = "내 카테고리 숙련도 목록")
  @GetMapping("/me/masteries")
  public ResponseEntity<ApiResponse<List<CategoryMasteryInfo>>> getMyMasteries(
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(ApiResponse.success(categoryMasteryService.getUserMasteries(userId)));
  }

  @Operation(summary = "카테고리별 숙련도 상위 유저")
  @GetMapping("/masteries/top")
  public ResponseEntity<ApiResponse<List<CategoryMasteryInfo>>> getTopMasters(
      @Parameter(description = "카테고리 ID") @RequestParam Long categoryId,
      @Parameter(description = "조회 수") @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(ApiResponse.success(categoryMasteryService.getTopMasters(categoryId, limit)));
  }
}
