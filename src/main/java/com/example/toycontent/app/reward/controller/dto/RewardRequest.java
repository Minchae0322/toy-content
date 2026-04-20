package com.example.toycontent.app.reward.controller.dto;

import com.example.toycontent.app.common.enumuration.MissionDifficulty;
import com.example.toycontent.app.reward.badge.domain.Badge;
import com.example.toycontent.app.reward.mission.domain.DailyMission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class RewardRequest {

  @Schema(description = "뱃지 생성 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateBadge {

    @Schema(description = "뱃지 코드", example = "BUY_PLACE_SHARER")
    @NotBlank(message = "뱃지 코드를 입력해주세요")
    @Size(max = 60)
    private String code;

    @Schema(description = "뱃지 이름", example = "구매처 쉐어러")
    @NotBlank(message = "뱃지 이름을 입력해주세요")
    @Size(max = 80)
    private String name;

    @Schema(description = "뱃지 설명")
    @Size(max = 300)
    private String description;

    @Schema(description = "아이콘 이모지", example = "🏪")
    @Size(max = 10)
    private String iconEmoji;

    @Schema(description = "아이콘 이미지 URL")
    @Size(max = 500)
    private String iconImageUrl;

    @Schema(description = "카테고리", example = "BRAG")
    @Size(max = 30)
    private String category;

    @Schema(description = "시즌 한정 여부")
    private boolean isSeasonal;

    @Schema(description = "시즌 코드")
    @Size(max = 20)
    private String seasonCode;

    public Badge toEntity() {
      return Badge.builder()
          .code(code)
          .name(name)
          .description(description)
          .iconEmoji(iconEmoji)
          .iconImageUrl(iconImageUrl)
          .category(category)
          .isSeasonal(isSeasonal)
          .seasonCode(seasonCode)
          .build();
    }
  }

  @Schema(description = "뱃지 수정 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateBadge {

    @Schema(description = "뱃지 이름")
    @NotBlank(message = "뱃지 이름을 입력해주세요")
    @Size(max = 80)
    private String name;

    @Schema(description = "뱃지 설명")
    @Size(max = 300)
    private String description;

    @Schema(description = "아이콘 이모지")
    @Size(max = 10)
    private String iconEmoji;

    @Schema(description = "아이콘 이미지 URL")
    @Size(max = 500)
    private String iconImageUrl;

    @Schema(description = "카테고리")
    @Size(max = 30)
    private String category;
  }

  @Schema(description = "일일 미션 생성 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateMission {

    @Schema(description = "미션 코드", example = "PRESS_FIRE_5")
    @NotBlank(message = "미션 코드를 입력해주세요")
    @Size(max = 60)
    private String code;

    @Schema(description = "미션 제목", example = "불꽃 5개 누르기")
    @NotBlank(message = "미션 제목을 입력해주세요")
    @Size(max = 120)
    private String title;

    @Schema(description = "미션 설명")
    @Size(max = 300)
    private String description;

    @Schema(description = "난이도", example = "EASY")
    @NotNull(message = "난이도를 선택해주세요")
    private String difficulty;

    @Schema(description = "목표 수", example = "5")
    @Positive(message = "목표 수는 1 이상이어야 합니다")
    private int targetCount;

    @Schema(description = "보상 EXP", example = "20")
    @Positive(message = "보상 EXP는 1 이상이어야 합니다")
    private int rewardExp;

    @Schema(description = "가챠 티켓 지급 여부")
    private boolean grantsGachaTicket;

    @Schema(description = "매일 고정 후보 여부")
    private boolean isFixedCandidate;

    public DailyMission toEntity() {
      return DailyMission.builder()
          .code(code)
          .title(title)
          .description(description)
          .difficulty(MissionDifficulty.valueOf(difficulty))
          .targetCount(targetCount)
          .rewardExp(rewardExp)
          .grantsGachaTicket(grantsGachaTicket)
          .isFixedCandidate(isFixedCandidate)
          .build();
    }
  }

  @Schema(description = "일일 미션 수정 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateMission {

    @Schema(description = "미션 제목")
    @NotBlank(message = "미션 제목을 입력해주세요")
    @Size(max = 120)
    private String title;

    @Schema(description = "미션 설명")
    @Size(max = 300)
    private String description;

    @Schema(description = "목표 수")
    @Positive
    private int targetCount;

    @Schema(description = "보상 EXP")
    @Positive
    private int rewardExp;
  }

  @Schema(description = "일일 미션 배정 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AssignMissions {

    @Schema(description = "배정할 미션 ID 목록")
    @NotNull(message = "미션 ID를 선택해주세요")
    private List<Long> missionIds;
  }
}
