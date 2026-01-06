package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.common.enumuration.FeedEvaluation;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class FeedRequest {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "피드 생성 요청")
  public static class CreateFeed {

    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @Schema(description = "상품 ID (기존 상품 선택 시)", example = "100")
    private Long productId;

    @Schema(description = "커스텀 상품명 (상품 ID가 없을 경우)", example = "스타벅스 아메리카노", maxLength = 200)
    @Size(max = 200, message = "상품명은 최대 200자까지 입력 가능합니다")
    private String productNameCustom;

    @Schema(description = "카테고리 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "카테고리는 필수입니다")
    private Long subCategoryId;

    @Schema(description = "리뷰 내용", example = "오늘 마신 커피가 정말 맛있었어요! 부드러운 맛이 일품입니다.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000)
    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(max = 1000, message = "리뷰 내용은 최대 1000자까지 입력 가능합니다")
    private String review;

    @Schema(description = "구매 가격 (원)", example = "4500", minimum = "0", maximum = "999999999")
    @Min(value = 0, message = "구매 가격은 0원 이상이어야 합니다")
    @Max(value = 999999999, message = "구매 가격은 999,999,999원 이하여야 합니다")
    private Integer buyPrice;

    @Schema(description = "정가 (원)", example = "5000", minimum = "0", maximum = "999999999")
    @Min(value = 0, message = "정가는 0원 이상이어야 합니다")
    @Max(value = 999999999, message = "정가는 999,999,999원 이하여야 합니다")
    private Integer price;

    @Schema(description = "구매처", example = "무신사", maxLength = 100)
    @NotBlank(message = "구매처는 필수입니다")
    @Size(max = 100, message = "구매처는 최대 100자까지 입력 가능합니다")
    private String buyPlace;

    @Schema(description = "제품 평가 (BEST/GOOD/OKAY/BAD)", example = "BEST")
    @NotBlank(message = "제품 평가는 필수입니다")
    private FeedEvaluation evaluation;

    @NotNull(message = "대표이미지는 필수 입니다.")
    @Schema(description = "대표이미지")
    private AttachmentFileRequest.AttachmentInfo thumbnailAttachmentInfo;

    @Schema(description = "첨부파일 정보 목록")
    private List<AttachmentInfo> attachmentFileInfos = new ArrayList<>();

    @Schema(description = "해시태그 목록 (# 제외, 최대 20개)", example = "[\"커피\", \"스타벅스\", \"아메리카노\"]", maxLength = 20)
    @Size(max = 20, message = "해시태그는 최대 20개까지 등록 가능합니다")
    private List<@Size(max = 50, message = "해시태그는 최대 50자까지 입력 가능합니다") String> hashtags;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "피드 수정 요청")
  public static class UpdateFeed {

    @Schema(description = "피드 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "피드 ID는 필수입니다")
    private Long feedId;

    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @Schema(description = "상품 ID (기존 상품 선택 시)", example = "100")
    private Long productId;

    @Schema(description = "커스텀 상품명", example = "스타벅스 아메리카노", maxLength = 200)
    @Size(max = 200, message = "상품명은 최대 200자까지 입력 가능합니다")
    private String productNameCustom;

    @Schema(description = "카테고리 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "카테고리는 필수입니다")
    private Long categoryId;

    @Schema(description = "리뷰 내용", example = "수정된 리뷰 내용입니다.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 1000)
    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(max = 1000, message = "리뷰 내용은 최대 1000자까지 입력 가능합니다")
    private String review;

    @Schema(description = "구매 가격 (원)", example = "4500", minimum = "0", maximum = "999999999")
    @Min(value = 0, message = "구매 가격은 0원 이상이어야 합니다")
    @Max(value = 999999999, message = "구매 가격은 999,999,999원 이하여야 합니다")
    private Integer buyPrice;

    @Schema(description = "정가 (원)", example = "5000", minimum = "0", maximum = "999999999")
    @Min(value = 0, message = "정가는 0원 이상이어야 합니다")
    @Max(value = 999999999, message = "정가는 999,999,999원 이하여야 합니다")
    private Integer price;

    @Schema(description = "해시태그 목록 (# 제외, 최대 20개)", example = "[\"커피\", \"맛집\"]", maxLength = 20)
    @Size(max = 20, message = "해시태그는 최대 20개까지 등록 가능합니다")
    private List<@Size(max = 50, message = "해시태그는 최대 50자까지 입력 가능합니다") String> hashtags;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SearchFeed {

    private Long categoryId;

    private List<String> hashtags;

    private String keyword;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    @Builder.Default
    private Integer size = 20;

    private String sortBy;
  }
}
