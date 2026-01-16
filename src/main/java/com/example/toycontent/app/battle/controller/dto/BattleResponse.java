package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.controller.dto.BattleVoteResponse.UserBattleVote;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse.SubCategoryDetail;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.app.product.controller.dto.ProductResponse.BattleItemProduct;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
  public static class BattleHotList {
    @Schema(description = "배틀 ID", example = "1")
    private Long id;

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    private String title;

    @Schema(description = "대표 이미지")
    private AttachmentFileResponse thumbnailDto;

    @Schema(description = "상위 아이템 이미지 목록 ((최대 4개) 대표이미지가 없으면)")
    private List<String> topItemImages;

    @Schema(description = "총 참여자 수", example = "48")
    private Integer totalParticipants;

    @Schema(description = "총 투표 수", example = "127")
    private Integer totalVotes;

    @Schema(description = "총 조회 수", example = "523")
    private Integer totalViews;

  }

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

    public static BattleDetail from(Battle battle, ExternalUserInfo userInfo, List<BattleItemInfo> items) {
      return BattleDetail.builder()
          .id(battle.getId())
          .title(battle.getTitle())
          .description(battle.getDescription())
          .subCategoryDetail(SubCategoryDetail.from(battle.getCategory()))
          .creatorUserInfo(userInfo)
          .itemAddPermissionType(battle.getItemAddPermissionType())
          .status(battle.getStatus())
          .startDate(battle.getStartDate())
          .endDate(battle.getEndDate())
          .participationStartDate(battle.getParticipationStartDate())
          .voteType(battle.getVoteType()).totalParticipants(battle.getTotalParticipants())
          .totalVotes(battle.getTotalVotes())
          .totalViews(battle.getTotalViews())
          .createdAt(battle.getCreatedAt())
          .items(items)
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
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BattleItemInfo implements Rankable {

    @Schema(description = "아이템 ID", example = "1")
    private Long id;

    @Schema(description = "제품 ID (커스텀 아이템인 경우 null)", example = "123")
    private BattleItemProduct battleItemProduct;

    @Schema(description = "제품명", example = "나이키 덩크 로우 판다")
    private String customName;

    @Schema(description = "브랜드", example = "Nike")
    private String customBrand;

    @Schema(description = "이미지 URL")
    private String customImageUrl;

    @Schema(description = "투표 수", example = "42")
    private Integer voteCount;

    @Schema(description = "득표율", example = "33.07")
    private Double votePercentage;

    @Schema(description = "득표 스코어", example = "22")
    private Integer totalScore;

    @Schema(description = "랭킹", example = "1")
    private Integer rank;

    @Schema(description = "아이템 상태", example = "ACTIVE")
    private BattleItemStatus status;

    @Schema(description = "신고 수", example = "0")
    private Integer reportCount;

    @Schema(description = "등록자 ID", example = "456")
    private Long registerId;

    @Schema(description = "사용자 투표 정보")
    private UserBattleVote userBattleVote;

    public static BattleItemInfo from(BattleItem item) {
      return BattleItemInfo.builder()
          .id(item.getId())
          .battleItemProduct(
              item.getProduct() != null ? BattleItemProduct.of(item.getProduct()) : null)
          .customName(item.getCustomName())
          .customBrand(item.getCustomBrand())
          .customImageUrl(item.getCustomImageUrl())
          .voteCount(item.getVoteCount())
          .totalScore(item.getTotalScore())
          .status(item.getStatus())
          .reportCount(item.getReportCount())
          .registerId(item.getRegisterId())
          .userBattleVote(item.getBattleVotes().stream()
              .findFirst()
              .map(UserBattleVote::from)
              .orElse(null))
          .votePercentage(
              item.getBattle().getTotalVotes() > 0
                  ? (double) item.getVoteCount() / item.getBattle().getTotalVotes()
                  : 0.0)
          .build();
    }

  }
}

