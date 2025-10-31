package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.SubCategoryDetail;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.ResultVisibility;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class BattleResponse {

  @Schema(description = "배틀 목록 조회 응답")
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleList {

    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    private String title;

    @Schema(description = "카테고리명", example = "스니커즈")
    private SubCategoryDetail subCategoryDetail;

    @Schema(description = "배틀 상태", example = "ACTIVE")
    private BattleStatus status;

    @Schema(description = "생성자 Dto", example = "스니커헤드123")
    private ExternalUserInfo creatorUserInfo;

    @Schema(description = "아이템 추가 권한 타입", example = "CREATOR_ONLY")
    private ItemAddPermissionType itemAddPermissionType;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

    @Schema(description = "시작일", example = "2025-02-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료일", example = "2025-02-28T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "생성일", example = "2025-01-25T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "대표 이미지")
    private AttachmentFileResponse thumbnailDto;

    @Schema(description = "상위 아이템 이미지 목록 ((최대 4개) 대표이미지가 없으면)")
    private List<String> topItemImages;

    @Schema(hidden = true)
    @JsonIgnore
    private Long creatorId;
  }

  @Schema(description = "배틀 상세 조회 응답")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleDetail {

    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    private String title;

    @Schema(description = "배틀 설명")
    private String description;

    @Schema(description = "카테고리명", example = "스니커즈")
    private SubCategoryDetail subCategoryDetail;

    @Schema(description = "배틀 상태", example = "ACTIVE")
    private BattleStatus status;

    @Schema(description = "생성자 정보", example = "스니커헤드123")
    private ExternalUserInfo creatorUserInfo;

    @NotNull(message = "아이템 추가 권한 타입을 선택해주세요")
    private ItemAddPermissionType itemAddPermissionType;

    @Schema(description = "생성자 프로필 이미지 URL")
    private String creatorProfileImageUrl;

    @Schema(description = "시작일", example = "2025-02-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료일", example = "2025-02-28T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "참여 시작일", example = "2025-02-01T00:00:00")
    private LocalDateTime participationStartDate;

    @Schema(description = "투표 타입", example = "SINGLE")
    private VoteType voteType;

    @Schema(description = "중복 제품 허용 여부", example = "true")
    private Boolean allowDuplicateProducts;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

    @Schema(description = "생성일", example = "2025-01-25T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "배틀 아이템 목록")
    private List<BattleItemInfo> items;

    @Schema(description = "추천 아이템 목록 (오픈 배틀)")
    private List<BattleItemInfo> suggestedItems;

    @Schema(description = "현재 사용자가 생성자인지 여부", example = "false")
    private Boolean isCreator;

    @Schema(description = "현재 사용자가 투표했는지 여부", example = "true")
    private Boolean hasVoted;

    public static BattleDetail from(Battle battle, ExternalUserInfo userInfo, boolean isCreator, boolean isHasVoted) {
      return BattleDetail.builder()
          .id(battle.getId())
          .title(battle.getTitle())
          .description(battle.getDescription())
          .subCategoryDetail(SubCategoryDetail.from(battle.getCategory()))
          .creatorUserInfo(userInfo)
          .status(battle.getStatus())
          .startDate(battle.getStartDate())
          .endDate(battle.getEndDate())
          .participationStartDate(battle.getParticipationStartDate())
          .voteType(battle.getVoteType())
          .allowDuplicateProducts(battle.getAllowDuplicateProducts())
          .totalParticipants(battle.getTotalParticipants())
          .totalVotes(battle.getTotalVotes())
          .totalViews(battle.getTotalViews())
          .createdAt(battle.getCreatedAt())
          .build();
    }
  }


  @Data
  @Schema(description = "배틀 생성 응답")
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleCreateResponse {

    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    public static BattleCreateResponse from(Battle battle) {
      return BattleCreateResponse.builder()
          .id(battle.getId())
          .build();
    }
  }

  @Schema(description = "배틀 아이템 정보")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleItemInfo {

    @Schema(description = "아이템 ID", example = "1")
    private Long id;

    @Schema(description = "제품 ID (커스텀 아이템인 경우 null)", example = "123")
    private Long productId;

    @Schema(description = "제품명", example = "나이키 덩크 로우 판다")
    private String name;

    @Schema(description = "브랜드", example = "Nike")
    private String brand;

    @Schema(description = "이모지", example = "👟")
    private String emoji;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "투표 수", example = "42")
    private Integer voteCount;

    @Schema(description = "현재 순위", example = "1")
    private Integer rank;

    @Schema(description = "득표율", example = "33.07")
    private Double votePercentage;

    @Schema(description = "커스텀 아이템 여부", example = "false")
    private Boolean isCustomItem;

    @Schema(description = "추천 아이템 여부", example = "false")
    private Boolean isSuggested;

    @Schema(description = "신규 아이템 여부 (NEW 뱃지)", example = "false")
    private Boolean isNew;

    @Schema(description = "NEW 뱃지 만료 시각")
    private LocalDateTime newBadgeExpiresAt;

    @Schema(description = "아이템 상태", example = "ACTIVE")
    private BattleItemStatus status;

    @Schema(description = "신고 수", example = "0")
    private Integer reportCount;

    @Schema(description = "등록자 ID (오픈 배틀)", example = "456")
    private Long registeredBy;

    @Schema(description = "등록자 닉네임 (오픈 배틀)", example = "슈즈러버")
    private String registeredByNickname;

    public static BattleItemInfo from(BattleItem item) {
      return BattleItemInfo.builder()
          .id(item.getId())
          .productId(item.getProduct() != null ? item.getProduct().getId() : null)
          .name(item.getName())
          .brand(item.getBrand())
          .imageUrl(item.getCustomImageUrl())
          .voteCount(item.getVoteCount())
          .isCustomItem(item.isCustomItem())
          .status(item.getStatus())
          .reportCount(item.getReportCount())
          .registeredBy(item.getRegisterId())
          .build();
    }
  }

  @Schema(description = "배틀 통계 응답")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Statistics {

    @Schema(description = "배틀 ID", example = "1")
    private Long battleId;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

    @Schema(description = "시간대별 투표 데이터")
    private List<HourlyVoteData> hourlyVotes;

    @Schema(description = "참여자 레벨 분포")
    private List<ParticipantLevelDistribution> levelDistribution;

    @Schema(description = "유입 경로 분석")
    private List<TrafficSource> trafficSources;

    @Schema(description = "피크 타임", example = "2025-02-15T20:00:00")
    private LocalDateTime peakVoteTime;

    @Schema(description = "평균 투표수 (1인당)", example = "2.65")
    private Double averageVotesPerUser;

    @Schema(description = "참여율", example = "85.42")
    private Double participationRate;
  }

  @Schema(description = "시간대별 투표 데이터")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HourlyVoteData {

    @Schema(description = "시간 (0~23)", example = "20")
    private Integer hour;

    @Schema(description = "투표 수", example = "15")
    private Integer voteCount;
  }

  @Schema(description = "참여자 레벨 분포")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ParticipantLevelDistribution {

    @Schema(description = "레벨 범위", example = "10-19")
    private String levelRange;

    @Schema(description = "인원 수", example = "12")
    private Integer count;

    @Schema(description = "비율", example = "25.0")
    private Double percentage;
  }

  @Schema(description = "유입 경로")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TrafficSource {

    @Schema(description = "유입 경로", example = "피드")
    private String source;

    @Schema(description = "유입 수", example = "230")
    private Integer count;

    @Schema(description = "비율", example = "43.98")
    private Double percentage;
  }
}

