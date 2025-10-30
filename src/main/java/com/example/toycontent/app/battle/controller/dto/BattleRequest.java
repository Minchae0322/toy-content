package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.ResultVisibility;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Size(min = 15, max = 50, message = "배틀 제목은 15~50자 사이여야 합니다")
    private String title;

    @Schema(description = "배틀 설명", example = "올해 출시된 스니커즈 중 디자인, 희소성, 가격을 종합적으로 고려하여 최고의 스니커즈를 선정합니다.")
    @Size(min = 120, max = 500, message = "배틀 설명은 120~500자 사이여야 합니다")
    private String description;

    @Schema(description = "카테고리 ID", example = "1")
    @NotNull(message = "카테고리를 선택해주세요")
    private Long categoryId;

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

    @Schema(description = "중복 제품 허용 여부", example = "true")
    @Builder.Default
    private Boolean allowDuplicateProducts = true;

    @Schema(description = "배틀 아이템 목록 (2~20개)")
    @Valid
    @Size(min = 2, max = 20, message = "아이템은 2~20개 사이여야 합니다")
    private List<ItemRequest> items;

    @NotNull(message = "대표이미지는 필수 입니다.")
    @Schema(description = "대표이미지")
    private AttachmentFileRequest.AttachmentInfo thumbnailAttachmentInfo;

  }

  @Schema(description = "배틀 아이템 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ItemRequest {

    @Schema(description = "제품 ID (기존 제품 선택 시)", example = "123")
    private Long productId;

    @Schema(description = "커스텀 제품명", example = "나이키 덩크 로우 판다")
    @Size(max = 30, message = "제품명은 30자 이내여야 합니다")
    private String customName;

    @Schema(description = "커스텀 브랜드", example = "Nike")
    @Size(max = 20, message = "브랜드명은 20자 이내여야 합니다")
    private String customBrand;

    @Schema(description = "커스텀 이미지 URL")
    @Size(max = 500, message = "이미지 URL은 500자 이내여야 합니다")
    private String customImageUrl;
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

  @Schema(description = "배틀 투표 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Vote {

    @Schema(description = "투표할 아이템 목록")
    @Valid
    @NotNull(message = "투표할 아이템을 선택해주세요")
    @Size(min = 1, max = 3, message = "투표는 1~3개까지 가능합니다")
    private List<VoteItem> votes;
  }

  @Schema(description = "투표 아이템")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VoteItem {

    @Schema(description = "아이템 ID", example = "1")
    @NotNull(message = "아이템을 선택해주세요")
    private Long itemId;

    @Schema(description = "순위 (1위=3점, 2위=2점, 3위=1점)", example = "1")
    @NotNull(message = "순위를 선택해주세요")
    @Min(value = 1, message = "순위는 1 이상이어야 합니다")
    @Max(value = 3, message = "순위는 3 이하여야 합니다")
    private Integer rank;
  }

  @Schema(description = "공지 등록 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddNotice {

    @Schema(description = "공지 내용", example = "마감 임박! 오늘 자정 종료됩니다")
    @NotBlank(message = "공지 내용을 입력해주세요")
    @Size(min = 10, max = 200, message = "공지는 10~200자 사이여야 합니다")
    private String message;
  }

  @Schema(description = "배틀 조기 종료 요청")
  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CloseBattle {

    @Schema(description = "종료 사유", example = "부적절한 콘텐츠 대량 등록으로 인한 조기 종료")
    @NotBlank(message = "종료 사유를 입력해주세요")
    @Size(min = 10, max = 200, message = "종료 사유는 10~200자 사이여야 합니다")
    private String reason;
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

