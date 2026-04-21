package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.service.dto.PreVoteUpdateCommand;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class BattleRequest {

  @Schema(description = "배틀 생성 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateBattle {

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    @NotBlank(message = "배틀 제목을 입력해주세요")
    @Size(min = 1, max = 50, message = "배틀 제목은 15~50자 사이여야 합니다")
    private String title;

    @Schema(description = "배틀 설명", example = "올해 출시된 스니커즈 중 디자인, 희소성, 가격을 종합적으로 고려하여 최고의 스니커즈를 선정합니다.")
    @Size(max = 500, message = "배틀 설명은 120~500자 사이여야 합니다")
    private String description;

    @Schema(description = "카테고리 ID", example = "1")
    @NotNull(message = "카테고리를 선택해주세요")
    private Long subCategoryId;

    @Schema(description = "아이템 추가 권한 타입", example = "CREATOR_ONLY")
    @NotNull(message = "아이템 추가 권한 타입을 선택해주세요")
    private ItemAddPermissionType itemAddPermissionType;

    @Schema(description = "시작일", example = "2025-02-01T00:00:00")
    @NotNull(message = "시작일을 입력해주세요")
    private LocalDateTime startDate;

    @Schema(description = "종료일", example = "2025-02-28T23:59:59")
    @NotNull(message = "종료일을 입력해주세요")
    private LocalDateTime endDate;

    @Schema(description = "참여 시작일", example = "2025-02-01T00:00:00")
    @NotNull(message = "참여 시작일을 입력해주세요")
    private LocalDateTime participationStartDate;

    @Schema(description = "투표 타입", example = "SINGLE")
    @NotNull(message = "투표 방식을 선택해주세요")
    private VoteType voteType;


    @Schema(description = "배틀 아이템 목록 (1~30개)")
    @Valid
    @Size(min = 1, max = 30, message = "아이템은 1~30개 사이여야 합니다")
    private List<ItemRequest> items;

    @Schema(description = "대표이미지")
    private AttachmentFileRequest.AttachmentInfo thumbnailAttachmentInfo;

  }

  @Schema(description = "배틀 아이템 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ItemRequest {

    @Schema(description = "아이템 타입 (PRODUCT: DB 등록 제품, CUSTOM: 사용자 직접입력, YOUTUBE: 유튜브 콘텐츠)",
        example = "PRODUCT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "아이템 타입은 필수입니다")
    private BattleItemType itemType;

    @Schema(description = "제품 ID (itemType=PRODUCT일 때 필수, 그 외 무시됨)", example = "123")
    private Long productId;

    @Schema(description = "아이템명 (itemType=CUSTOM, YOUTUBE일 때 필수)", example = "나이키 덩크 로우 판다")
    @Size(max = 30, message = "제품명은 30자 이내여야 합니다")
    private String customName;

    @Schema(description = "브랜드명 (itemType=CUSTOM, YOUTUBE일 때 선택)", example = "Nike")
    @Size(max = 20, message = "브랜드명은 20자 이내여야 합니다")
    private String customBrand;

    @Schema(description = "이미지 URL (itemType=CUSTOM일 때 선택, 그 외 무시됨)")
    @Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다")
    private String customImageUrl;

    @Schema(description = "유튜브 URL (itemType=YOUTUBE일 때 필수, 그 외 무시됨). "
        + "지원 형식: youtube.com/watch, youtu.be, youtube.com/shorts",
        example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    @Size(max = 500, message = "콘텐츠 URL은 500자 이내여야 합니다")
    private String contentUrl;
  }

  @Schema(description = "배틀 수정 요청 (부분 수정, null 필드는 변경하지 않음)")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateBattle {

    @Schema(description = "배틀 제목", example = "2025년 최고의 스니커즈를 찾아라")
    @Size(min = 1, max = 50, message = "배틀 제목은 1~50자 사이여야 합니다")
    private String title;

    @Schema(description = "배틀 설명")
    @Size(max = 500, message = "배틀 설명은 500자 이내여야 합니다")
    private String description;

    @Schema(description = "카테고리 ID (투표 시작 전에만 수정 가능)", example = "1")
    private Long subCategoryId;

    @Schema(description = "아이템 추가 권한 타입 (투표 시작 전에만 수정 가능)", example = "CREATOR_ONLY")
    private ItemAddPermissionType itemAddPermissionType;

    @Schema(description = "시작일 (투표 시작 전에만 수정 가능)", example = "2025-02-01T00:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료일 (투표 시작 전에만 수정 가능)", example = "2025-02-28T23:59:59")
    private LocalDateTime endDate;

    @Schema(description = "참여 시작일 (투표 시작 전에만 수정 가능)", example = "2025-02-01T00:00:00")
    private LocalDateTime participationStartDate;

    @Schema(description = "투표 타입 (투표 시작 전에만 수정 가능)", example = "SINGLE")
    private VoteType voteType;

    public PreVoteUpdateCommand toPreVoteCommand() {
      return new PreVoteUpdateCommand(
          itemAddPermissionType,
          startDate,
          endDate,
          participationStartDate,
          voteType);
    }
  }

  @Schema(description = "배틀 아이템 추가 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddBattleItems {

    @Schema(description = "추가할 아이템 목록 (1~3개)")
    @Valid
    @NotNull(message = "추가할 아이템을 선택해주세요")
    @Size(min = 1, max = 3, message = "한 번에 1~3개까지 추가 가능합니다")
    private List<ItemRequest> items;
  }


  @Schema(description = "신고 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Report {

    @Schema(description = "신고 사유", example = "부적절한 이미지가 포함되어 있습니다")
    @NotBlank(message = "신고 사유를 입력해주세요")
    @Size(min = 10, max = 200, message = "신고 사유는 10~200자 사이여야 합니다")
    private String reason;
  }
}

