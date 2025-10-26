package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.common.enumuration.FeedEvaluation;
import com.example.toycontent.app.product.controller.dto.ProductResponse;
import com.example.toycontent.app.product.controller.dto.ProductResponse.FeedProduct;
import com.example.toycontent.app.feed.controller.dto.FeedReactionResponse.ReactionStats;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedHashtag;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public abstract class FeedResponse {

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "피드 목록 조회 응답")
  public static class ListView {

    @Schema(description = "피드 ID")
    private Long feedId;

    @Schema(description = "사용자 ID")
    private ExternalUserInfo userInfo;

    @Schema(description = "상품 ID")
    private Long productId;

    @Schema(description = "상품명 (커스텀)")
    private String productName;

    @Schema(description = "카테고리 ID")
    private Long categoryId;

    @Schema(description = "카테고리명")
    private String categoryName;

    @Schema(description = "리뷰 내용 (요약)")
    private String reviewSummary;

    @Schema(description = "구매 가격")
    private Integer buyPrice;

    @Schema(description = "정가")
    private Integer price;

    @Schema(description = "조회수")
    private Integer viewCount;

    @Schema(description = "대표 이미지 URL")
    private AttachmentFileResponse thumbnailUrl;

    @Schema(description = "해시태그 목록")
    private List<String> hashtags;

    @Schema(description = "반응 통계")
    private ReactionStats reactionStats;

    @Schema(description = "구매처", example = "무신사")
    private String buyPlace;

    @Schema(description = "제품 평가 (BEST/GOOD/OKAY/BAD)", example = "BEST")
    private FeedEvaluation feedEvaluation;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    public static ListView from(Feed feed, ExternalUserInfo userInfo) {
      return ListView.builder()
          .feedId(feed.getId())
          .userInfo(userInfo)
          .productId(feed.getProduct() != null ? feed.getProduct().getId() : null)
          .productName(
              feed.getProduct() != null ? feed.getProduct().getName() : feed.getProductNameCustom())
          .reviewSummary(truncateReview(feed.getReview(), 100))
          .buyPrice(feed.getBuyPrice())
          .price(feed.getPrice())
          .viewCount(feed.getViewCount())
          .thumbnailUrl(
              feed.getAttachmentFiles()
                  .stream()
                  .findFirst()
                  .map(AttachmentFileResponse::of)
                  .orElse(null)
          )
          .hashtags(extractHashtags(feed.getHashtags()))
          .buyPlace(feed.getBuyPlace())
          .reactionStats(ReactionStats.from(feed.getReactions()))
          .feedEvaluation(feed.getEvaluation())
          .createdAt(feed.getCreatedAt())
          .updatedAt(feed.getUpdatedAt())
          .build();
    }

    private static String truncateReview(String review, int maxLength) {
      if (review == null || review.length() <= maxLength) {
        return review;
      }
      return review.substring(0, maxLength) + "...";
    }

    private static List<String> extractHashtags(List<FeedHashtag> feedHashtags) {
      if (feedHashtags == null || feedHashtags.isEmpty()) {
        return List.of();
      }
      return feedHashtags.stream()
          .map(fh -> fh.getHashtag().getName())
          .collect(Collectors.toList());
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "피드 상세 조회 응답")
  public static class Detail {

    @Schema(description = "피드 ID")
    private Long feedId;

    @Schema(description = "사용자 ID")
    private Long userId;

    @Schema(description = "상품 정보")
    private ProductResponse.FeedProduct product;

    @Schema(description = "카테고리 ID")
    private Long categoryId;

    @Schema(description = "카테고리명")
    private String categoryName;

    @Schema(description = "리뷰 내용")
    private String review;

    @Schema(description = "구매 가격")
    private Integer buyPrice;

    @Schema(description = "정가")
    private Integer price;

    @Schema(description = "조회수")
    private Integer viewCount;

    @Schema(description = "첨부 파일 목록")
    private List<AttachmentFileResponse> attachmentFiles;

    @Schema(description = "해시태그 목록")
    private List<HashtagInfo> hashtags;

    @Schema(description = "반응 통계")
    private ReactionStats reactionStats;

    @Schema(description = "제품 평가 (BEST/GOOD/OKAY/BAD)", example = "BEST")
    private FeedEvaluation feedEvaluation;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    public static Detail from(Feed feed) {
      return Detail.builder()
          .feedId(feed.getId())
          .userId(feed.getUserId())
          .product(feed.getProduct() != null ? FeedProduct.of(feed.getProduct()) : null)
          .categoryId(feed.getCategory() != null ? feed.getCategory().getId() : null)
          .categoryName(feed.getCategory() != null ? feed.getCategory().getName() : null)
          .review(feed.getReview())
          .buyPrice(feed.getBuyPrice())
          .price(feed.getPrice())
          .viewCount(feed.getViewCount())
          .attachmentFiles(
              feed.getAttachmentFiles()
                  .stream()
                  .map(AttachmentFileResponse::of)
                  .toList())
          .hashtags(HashtagInfo.fromList(feed.getHashtags()))
          .reactionStats(ReactionStats.from(feed.getReactions()))
          .feedEvaluation(feed.getEvaluation())
          .createdAt(feed.getCreatedAt())
          .updatedAt(feed.getUpdatedAt())
          .build();
    }

  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "핫 피드 응답")
  public static class HotFeedResponse {

    @Schema(description = "피드 ID")
    private Long feedId;

    @Schema(description = "상품명")
    private String productName;

    @Schema(description = "리뷰 내용 (요약)")
    private String reviewSummary;

    @Schema(description = "대표 이미지 URL")
    private AttachmentFileResponse hotFeedThumbnailDto;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "해시태그 정보")
  public static class HashtagInfo {

    @Schema(description = "해시태그 ID")
    private Long hashtagId;

    @Schema(description = "해시태그명")
    private String name;

    public static HashtagInfo from(FeedHashtag feedHashtag) {
      return HashtagInfo.builder()
          .hashtagId(feedHashtag.getHashtag().getId())
          .name(feedHashtag.getHashtag().getName())
          .build();
    }

    public static List<HashtagInfo> fromList(List<FeedHashtag> feedHashtags) {
      if (feedHashtags == null || feedHashtags.isEmpty()) {
        return List.of();
      }
      return feedHashtags.stream()
          .map(HashtagInfo::from)
          .collect(Collectors.toList());
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "피드 목록 응답 (커서 페이징)")
  public static class FeedCursorResponse {

    @Schema(description = "피드 목록")
    private List<FeedResponse.ListView> feeds;

    @Schema(description = "다음 커서 (다음 페이지 요청 시 사용)")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부")
    private Boolean hasNext;

    @Schema(description = "조회된 피드 개수")
    private Integer size;

    public static FeedCursorResponse of(List<FeedResponse.ListView> feeds, Integer requestSize) {
      boolean hasNext = feeds.size() > requestSize;

      // size+1개를 조회했으므로, 실제로는 size개만 반환
      List<FeedResponse.ListView> actualFeeds = hasNext
          ? feeds.subList(0, requestSize)
          : feeds;

      Long nextCursor = hasNext
          ? actualFeeds.get(actualFeeds.size() - 1).getFeedId()
          : null;

      return FeedCursorResponse.builder()
          .feeds(actualFeeds)
          .nextCursor(nextCursor)
          .hasNext(hasNext)
          .size(actualFeeds.size())
          .build();
    }
  }



}
