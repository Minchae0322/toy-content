package com.example.toycontent.app.reward.controller.dto;

import com.example.toycontent.app.common.enumuration.CategoryMasteryTier;
import com.example.toycontent.app.common.enumuration.MissionDifficulty;
import com.example.toycontent.app.common.enumuration.MissionProgressStatus;
import com.example.toycontent.app.reward.domain.Badge;
import com.example.toycontent.app.reward.domain.BattlePrediction;
import com.example.toycontent.app.reward.domain.CategoryMastery;
import com.example.toycontent.app.reward.domain.DailyMission;
import com.example.toycontent.app.reward.domain.UserBadge;
import com.example.toycontent.app.reward.domain.UserDailyMissionAssignment;
import com.example.toycontent.app.reward.domain.UserReward;
import com.example.toycontent.app.reward.domain.UserStreak;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class RewardResponse {

  @Schema(description = "유저 EXP/레벨 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserRewardInfo {

    @Schema(description = "현재 레벨", example = "5")
    private Integer level;

    @Schema(description = "누적 총 EXP", example = "1500")
    private Long totalExp;

    @Schema(description = "현재 레벨에서 쌓인 EXP", example = "30")
    private Long currentLevelExp;

    @Schema(description = "다음 레벨까지 필요 EXP", example = "500")
    private Long nextLevelExp;

    @Schema(description = "시즌 EXP", example = "200")
    private Long seasonExp;

    @Schema(description = "시즌 코드", example = "2026-Q2")
    private String seasonCode;

    public static UserRewardInfo from(UserReward entity) {
      return UserRewardInfo.builder()
          .level(entity.getLevel())
          .totalExp(entity.getTotalExp())
          .currentLevelExp(entity.getCurrentLevelExp())
          .nextLevelExp(entity.getNextLevelExp())
          .seasonExp(entity.getSeasonExp())
          .seasonCode(entity.getSeasonCode())
          .build();
    }
  }

  @Schema(description = "뱃지 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BadgeInfo {

    @Schema(description = "뱃지 ID")
    private Long id;

    @Schema(description = "뱃지 코드", example = "BUY_PLACE_SHARER")
    private String code;

    @Schema(description = "뱃지 이름", example = "구매처 쉐어러")
    private String name;

    @Schema(description = "뱃지 설명")
    private String description;

    @Schema(description = "아이콘 이모지", example = "🏪")
    private String iconEmoji;

    @Schema(description = "아이콘 이미지 URL")
    private String iconImageUrl;

    @Schema(description = "카테고리", example = "BRAG")
    private String category;

    public static BadgeInfo from(Badge entity) {
      return BadgeInfo.builder()
          .id(entity.getId())
          .code(entity.getCode())
          .name(entity.getName())
          .description(entity.getDescription())
          .iconEmoji(entity.getIconEmoji())
          .iconImageUrl(entity.getIconImageUrl())
          .category(entity.getCategory())
          .build();
    }
  }

  @Schema(description = "유저 뱃지 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserBadgeInfo {

    @Schema(description = "유저 뱃지 ID")
    private Long id;

    @Schema(description = "뱃지 정보")
    private BadgeInfo badge;

    @Schema(description = "획득 일시")
    private LocalDateTime acquiredAt;

    @Schema(description = "프로필 고정 여부")
    private Boolean pinned;

    public static UserBadgeInfo from(UserBadge entity) {
      return UserBadgeInfo.builder()
          .id(entity.getId())
          .badge(BadgeInfo.from(entity.getBadge()))
          .acquiredAt(entity.getAcquiredAt())
          .pinned(entity.getPinned())
          .build();
    }
  }

  @Schema(description = "스트릭 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserStreakInfo {

    @Schema(description = "현재 연속 일수", example = "7")
    private Integer currentStreak;

    @Schema(description = "역대 최고 연속 일수", example = "30")
    private Integer maxStreak;

    @Schema(description = "마지막 작성일")
    private LocalDate lastPostedDate;

    @Schema(description = "복구 티켓 수", example = "2")
    private Integer recoveryTickets;

    @Schema(description = "마지막 마일스톤", example = "7")
    private Integer lastMilestoneReached;

    public static UserStreakInfo from(UserStreak entity) {
      return UserStreakInfo.builder()
          .currentStreak(entity.getCurrentStreak())
          .maxStreak(entity.getMaxStreak())
          .lastPostedDate(entity.getLastPostedDate())
          .recoveryTickets(entity.getRecoveryTickets())
          .lastMilestoneReached(entity.getLastMilestoneReached())
          .build();
    }
  }

  @Schema(description = "일일 미션 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyMissionInfo {

    @Schema(description = "미션 ID")
    private Long id;

    @Schema(description = "미션 코드")
    private String code;

    @Schema(description = "미션 제목")
    private String title;

    @Schema(description = "미션 설명")
    private String description;

    @Schema(description = "난이도")
    private MissionDifficulty difficulty;

    @Schema(description = "목표 수")
    private Integer targetCount;

    @Schema(description = "보상 EXP")
    private Integer rewardExp;

    @Schema(description = "가챠 티켓 지급 여부")
    private Boolean grantsGachaTicket;

    public static DailyMissionInfo from(DailyMission entity) {
      return DailyMissionInfo.builder()
          .id(entity.getId())
          .code(entity.getCode())
          .title(entity.getTitle())
          .description(entity.getDescription())
          .difficulty(entity.getDifficulty())
          .targetCount(entity.getTargetCount())
          .rewardExp(entity.getRewardExp())
          .grantsGachaTicket(entity.getGrantsGachaTicket())
          .build();
    }
  }

  @Schema(description = "미션 배정 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MissionAssignmentInfo {

    @Schema(description = "배정 ID")
    private Long id;

    @Schema(description = "미션 정보")
    private DailyMissionInfo mission;

    @Schema(description = "배정 일자")
    private LocalDate assignedDate;

    @Schema(description = "현재 진행 수")
    private Integer currentCount;

    @Schema(description = "목표 수")
    private Integer targetCount;

    @Schema(description = "진행 상태")
    private MissionProgressStatus status;

    @Schema(description = "완료 일시")
    private LocalDateTime completedAt;

    @Schema(description = "보상 수령 일시")
    private LocalDateTime claimedAt;

    public static MissionAssignmentInfo from(UserDailyMissionAssignment entity) {
      return MissionAssignmentInfo.builder()
          .id(entity.getId())
          .mission(DailyMissionInfo.from(entity.getMission()))
          .assignedDate(entity.getAssignedDate())
          .currentCount(entity.getCurrentCount())
          .targetCount(entity.getTargetCount())
          .status(entity.getStatus())
          .completedAt(entity.getCompletedAt())
          .claimedAt(entity.getClaimedAt())
          .build();
    }
  }

  @Schema(description = "배틀 예측 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PredictionInfo {

    @Schema(description = "예측 ID")
    private Long id;

    @Schema(description = "배틀 ID")
    private Long battleId;

    @Schema(description = "예측 아이템 ID")
    private Long predictedItemId;

    @Schema(description = "우승 아이템 ID")
    private Long winnerItemId;

    @Schema(description = "적중 여부")
    private Boolean hit;

    @Schema(description = "판정 일시")
    private LocalDateTime settledAt;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    public static PredictionInfo from(BattlePrediction entity) {
      return PredictionInfo.builder()
          .id(entity.getId())
          .battleId(entity.getBattle().getId())
          .predictedItemId(entity.getPredictedItem().getId())
          .winnerItemId(entity.getWinnerItem() != null ? entity.getWinnerItem().getId() : null)
          .hit(entity.getHit())
          .settledAt(entity.getSettledAt())
          .createdAt(entity.getCreatedAt())
          .build();
    }
  }

  @Schema(description = "카테고리 숙련도 정보")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CategoryMasteryInfo {

    @Schema(description = "카테고리 ID")
    private Long categoryId;

    @Schema(description = "카테고리 이름")
    private String categoryName;

    @Schema(description = "현재 등급")
    private CategoryMasteryTier tier;

    @Schema(description = "피드 수")
    private Integer feedCount;

    @Schema(description = "배틀 투표 수")
    private Integer battleVoteCount;

    @Schema(description = "PICK 댓글 수")
    private Integer pickCommentCount;

    @Schema(description = "예측 정확도")
    private Double predictionAccuracy;

    public static CategoryMasteryInfo from(CategoryMastery entity) {
      return CategoryMasteryInfo.builder()
          .categoryId(entity.getCategory().getId())
          .categoryName(entity.getCategory().getName())
          .tier(entity.getTier())
          .feedCount(entity.getFeedCount())
          .battleVoteCount(entity.getBattleVoteCount())
          .pickCommentCount(entity.getPickCommentCount())
          .predictionAccuracy(entity.getPredictionAccuracy())
          .build();
    }
  }
}
