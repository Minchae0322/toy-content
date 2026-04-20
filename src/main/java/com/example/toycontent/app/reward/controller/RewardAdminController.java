package com.example.toycontent.app.reward.controller;

import com.example.toycontent.app.common.annotation.CheckAdmin;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.reward.controller.dto.RewardRequest;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.BadgeInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.DailyMissionInfo;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.ExpHistoryInfo;
import com.example.toycontent.app.reward.badge.service.BadgeService;
import com.example.toycontent.app.reward.mission.service.DailyMissionService;
import com.example.toycontent.app.reward.exp.service.UserRewardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RewardAdminController", description = "보상 관리 API")
@RestController
@RequestMapping("/rewards/admin")
@RequiredArgsConstructor
public class RewardAdminController {

  private final BadgeService badgeService;
  private final DailyMissionService dailyMissionService;
  private final UserRewardService userRewardService;

  // ── 뱃지 관리 ──

  @CheckAdmin
  @Operation(summary = "뱃지 생성")
  @PostMapping("/badges")
  public ResponseEntity<ApiResponse<BadgeInfo>> createBadge(
      @Valid @RequestBody RewardRequest.CreateBadge request) {
    return ResponseEntity.ok(ApiResponse.success(badgeService.createBadge(request), "뱃지가 생성되었습니다."));
  }

  @CheckAdmin
  @Operation(summary = "뱃지 수정")
  @PutMapping("/badges/{badgeId}")
  public ResponseEntity<ApiResponse<BadgeInfo>> updateBadge(
      @Parameter(description = "뱃지 ID") @PathVariable Long badgeId,
      @Valid @RequestBody RewardRequest.UpdateBadge request) {
    return ResponseEntity.ok(ApiResponse.success(badgeService.updateBadge(badgeId, request), "뱃지가 수정되었습니다."));
  }

  @CheckAdmin
  @Operation(summary = "뱃지 비활성화")
  @DeleteMapping("/badges/{badgeId}")
  public ResponseEntity<ApiResponse<Void>> deactivateBadge(
      @Parameter(description = "뱃지 ID") @PathVariable Long badgeId) {
    badgeService.deactivateBadge(badgeId);
    return ResponseEntity.ok(ApiResponse.success(null, "뱃지가 비활성화되었습니다."));
  }

  @CheckAdmin
  @Operation(summary = "활성 뱃지 목록 조회")
  @GetMapping("/badges")
  public ResponseEntity<ApiResponse<List<BadgeInfo>>> getActiveBadges() {
    return ResponseEntity.ok(ApiResponse.success(badgeService.getActiveBadges()));
  }

  @CheckAdmin
  @Operation(summary = "카테고리별 뱃지 조회")
  @GetMapping("/badges/category")
  public ResponseEntity<ApiResponse<List<BadgeInfo>>> getBadgesByCategory(
      @Parameter(description = "카테고리") @RequestParam String category) {
    return ResponseEntity.ok(ApiResponse.success(badgeService.getBadgesByCategory(category)));
  }

  // ── 미션 관리 ──

  @CheckAdmin
  @Operation(summary = "일일 미션 생성")
  @PostMapping("/missions")
  public ResponseEntity<ApiResponse<DailyMissionInfo>> createMission(
      @Valid @RequestBody RewardRequest.CreateMission request) {
    return ResponseEntity.ok(ApiResponse.success(dailyMissionService.createMission(request), "미션이 생성되었습니다."));
  }

  @CheckAdmin
  @Operation(summary = "일일 미션 수정")
  @PutMapping("/missions/{missionId}")
  public ResponseEntity<ApiResponse<DailyMissionInfo>> updateMission(
      @Parameter(description = "미션 ID") @PathVariable Long missionId,
      @Valid @RequestBody RewardRequest.UpdateMission request) {
    return ResponseEntity.ok(ApiResponse.success(dailyMissionService.updateMission(missionId, request), "미션이 수정되었습니다."));
  }

  @CheckAdmin
  @Operation(summary = "일일 미션 비활성화")
  @DeleteMapping("/missions/{missionId}")
  public ResponseEntity<ApiResponse<Void>> deactivateMission(
      @Parameter(description = "미션 ID") @PathVariable Long missionId) {
    dailyMissionService.deactivateMission(missionId);
    return ResponseEntity.ok(ApiResponse.success(null, "미션이 비활성화되었습니다."));
  }

  @CheckAdmin
  @Operation(summary = "활성 미션 목록 조회")
  @GetMapping("/missions")
  public ResponseEntity<ApiResponse<List<DailyMissionInfo>>> getActiveMissions() {
    return ResponseEntity.ok(ApiResponse.success(dailyMissionService.getActiveMissions()));
  }

  @CheckAdmin
  @Operation(summary = "고정 후보 미션 조회")
  @GetMapping("/missions/fixed-candidates")
  public ResponseEntity<ApiResponse<List<DailyMissionInfo>>> getFixedCandidateMissions() {
    return ResponseEntity.ok(ApiResponse.success(dailyMissionService.getFixedCandidateMissions()));
  }

  // ── EXP 이력 관리 ──

  @CheckAdmin
  @Operation(summary = "유저 EXP 이력 조회")
  @GetMapping("/exp-history/{userId}")
  public ResponseEntity<ApiResponse<Page<ExpHistoryInfo>>> getExpHistory(
      @Parameter(description = "유저 ID") @PathVariable Long userId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.success(userRewardService.getExpHistory(userId, pageable).map(ExpHistoryInfo::from)));
  }
}
