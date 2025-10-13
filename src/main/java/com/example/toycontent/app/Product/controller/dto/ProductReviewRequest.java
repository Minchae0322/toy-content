package com.example.toycontent.app.Product.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class ProductReviewRequest {

  /**
   * 리뷰 등록 요청
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "리뷰 등록 요청")
  public static class CreateReview {

    @Schema(description = "상품 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @Schema(description = "평점 (1-5)", example = "5", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    private Integer rating;

    @Schema(
        description = "리뷰 내용 (10-1000자)",
        example = "맛있고 가성비도 좋아요. 편의점에서 자주 구매하는 제품입니다. 강력 추천합니다!",
        minLength = 10,
        maxLength = 1000,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(min = 10, max = 1000, message = "리뷰 내용은 10자 이상 1000자 이하여야 합니다")
    private String comment;

    @Schema(
        description = "리뷰 이미지 URL 목록 (최대 5장)",
        example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]",
        maxLength = 5
    )
    @Size(max = 5, message = "이미지는 최대 5장까지 첨부 가능합니다")
    private List<String> imageUrls;
  }

  /**
   * 리뷰 수정 요청
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "리뷰 수정 요청")
  public static class UpdateReview {

    @Schema(description = "평점 (1-5)", example = "4", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    private Integer rating;

    @Schema(
        description = "리뷰 내용 (10-1000자)",
        example = "재구매 의사가 있습니다. 맛도 좋고 가격도 적당해요.",
        minLength = 10,
        maxLength = 1000,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(min = 10, max = 1000, message = "리뷰 내용은 10자 이상 1000자 이하여야 합니다")
    private String comment;

    @Schema(
        description = "리뷰 이미지 URL 목록 (최대 5장)",
        example = "[\"https://example.com/image1.jpg\"]",
        maxLength = 5
    )
    @Size(max = 5, message = "이미지는 최대 5장까지 첨부 가능합니다")
    private List<String> imageUrls;
  }

  /**
   * 리뷰 신고 요청
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "리뷰 신고 요청")
  public static class ReportReview {

    @Schema(
        description = "신고 사유 (10-500자)",
        example = "부적절한 내용이 포함되어 있습니다. 욕설과 비방이 있어 신고합니다.",
        minLength = 10,
        maxLength = 500,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "신고 사유는 필수입니다")
    @Size(min = 10, max = 500, message = "신고 사유는 10자 이상 500자 이하여야 합니다")
    private String reason;

    @Schema(description = "신고자 이메일 (선택)", example = "reporter@example.com")
    private String reporterEmail;
  }

  /**
   * 리뷰 목록 조회 조건
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "리뷰 검색 조건")
  public static class SearchCondition {

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "작성자 ID", example = "1")
    private Long creatorId;

    @Schema(description = "평점 필터 (1-5)", example = "5", minimum = "1", maximum = "5")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    private Integer rating;

    @Schema(
        description = "리뷰 상태",
        example = "ACTIVE",
        allowableValues = {"ACTIVE", "DELETED", "REPORTED", "BLOCKED"}
    )
    private String status;

    @Schema(
        description = "정렬 기준",
        example = "createdAt",
        allowableValues = {"createdAt", "rating", "likeCount"}
    )
    private String sortBy;

    @Schema(
        description = "정렬 방향",
        example = "DESC",
        allowableValues = {"ASC", "DESC"}
    )
    private String sortDirection;
  }

}
