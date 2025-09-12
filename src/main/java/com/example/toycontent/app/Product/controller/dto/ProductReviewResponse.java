package com.example.toycontent.app.Product.controller.dto;

import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.common.enumuration.ReviewStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
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

    @Schema(description = "상품 ID", example = "100")
    private Long productId;

    @Schema(description = "상품명", example = "건담 로봇")
    private String productName;

    @Schema(description = "리뷰 작성자 ID", example = "200")
    private Long creatorId;

    @Schema(description = "작성자 닉네임", example = "건담매니아")
    private String creatorNickname;

    @Schema(description = "평점", example = "5")
    private Integer rating;

    @Schema(description = "리뷰 내용", example = "정말 멋진 건담입니다! 디테일이 훌륭해요.")
    private String comment;

    @Schema(description = "리뷰 상태", example = "ACTIVE")
    private ReviewStatus status;

    @Schema(description = "좋아요 수", example = "8")
    private Integer likeCount;

    @Schema(description = "신고 수", example = "0")
    private Integer reportCount;

    @Schema(description = "리뷰 작성일", example = "2024-01-15T14:20:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "리뷰 수정일", example = "2024-01-16T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "사용자의 좋아요 여부", example = "false")
    private Boolean isLikedByUser;

    @Schema(description = "본인 작성 리뷰 여부", example = "true")
    private Boolean isOwnReview;

    /**
     * ProductReview 엔티티로부터 ReviewList DTO 생성
     */
    public static ReviewList of(ProductReview review, String creatorNickname, Boolean isLikedByUser, Boolean isOwnReview) {
      return ReviewList.builder()
          .id(review.getId())
          .productId(review.getProduct().getId())
          .productName(review.getProduct().getName())
          .creatorId(review.getCreatorId())
          .creatorNickname(creatorNickname != null ? creatorNickname : "사용자" + review.getCreatorId())
          .rating(review.getRating())
          .comment(review.getComment())
          .status(review.getStatus())
          .likeCount(review.getLikeCount())
          .reportCount(review.getReportCount())
          .createdAt(review.getCreatedAt())
          .updatedAt(review.getUpdatedAt())
          .isLikedByUser(isLikedByUser)
          .isOwnReview(isOwnReview)
          .build();
    }
  }
}
