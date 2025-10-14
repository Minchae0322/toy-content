package com.example.toycontent.app.Product.controller.dto;

import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class ProductReviewResponse {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "리뷰 목록 응답")
  public static class ReviewList {

    @Schema(description = "리뷰 ID", example = "1")
    private Long id;

    @Schema(description = "작성자 ID", example = "123")
    private Long creatorId;

    @Schema(description = "작성자 닉네임", example = "홍길동")
    private String creatorName;

    @Schema(description = "평점", example = "5")
    private Integer rating;

    @Schema(description = "리뷰 내용", example = "정말 좋은 제품이에요!")
    private String comment;

    @Schema(description = "리뷰 상태", example = "ACTIVE")
    private ReviewStatus status;

    @Schema(description = "좋아요 수", example = "10")
    private Integer likeCount;

    @Schema(description = "신고 수", example = "0")
    private Integer reportCount;

    @Schema(description = "작성일시", example = "2024-03-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-03-15T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "리뷰 첨부파일 목록")
    private List<AttachmentFileResponse> attachmentFiles;

    public static ReviewList of(ProductReview review) {
      return ReviewList.builder()
          .id(review.getId())
          .creatorId(review.getCreatorId())
          .creatorName(review.getCreatorName())
          .rating(review.getRating())
          .comment(review.getComment())
          .status(review.getStatus())
          .likeCount(review.getLikeCount())
          .reportCount(review.getReportCount())
          .createdAt(review.getCreatedAt())
          .updatedAt(review.getUpdatedAt())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "리뷰 목록 응답")
  public static class ReviewCreateResponse {

    @Schema(description = "리뷰 ID", example = "1")
    private Long productReviewId;

    @Schema(description = "제품 ID", example = "1")
    private Long productId;

    public static ReviewCreateResponse of(ProductReview review) {
      return ReviewCreateResponse.builder()
          .productReviewId(review.getId())
          .productId(review.getProduct().getId())
          .build();
    }
  }
}
