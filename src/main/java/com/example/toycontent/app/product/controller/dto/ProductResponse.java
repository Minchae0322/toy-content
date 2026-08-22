package com.example.toycontent.app.product.controller.dto;

import static com.example.toycontent.app.common.utils.BattleItemRankingCalculator.setRanking;
import static com.example.toycontent.app.common.utils.TagParsingUtil.parseToList;

import com.example.toycontent.app.battle.controller.dto.Rankable;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.category.contoller.dto.CategoryResponse;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.app.product.controller.dto.ProductReactionResponse.ProductUserReaction;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;


public abstract class ProductResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 목록 응답")
    public static class ProductList implements java.io.Serializable {

    // Redis 캐시(JDK 직렬화) 대상 - UID 고정으로 배포 간 호환 유지
    private static final long serialVersionUID = 1L;
        @Schema(description = "상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "건담 로봇")
        private String name;

        @Schema(description = "브랜드명", example = "반다이")
        private String brand;

        @Schema(description = "상품 상태", example = "APPROVED")
        private ProductStatus status;

        @Schema(description = "상품 정가", example = "25000")
        private String price;

        @Schema(description = "조회수", example = "150")
        private Integer viewCount;

        @Schema(description = "찜하기 수", example = "23")
        private Integer likeCount;

        @Schema(description = "평균 평점", example = "4.5")
        private Double averageRating;

        @Schema(description = "리뷰 수", example = "12")
        private Integer reviewCount;

        @Schema(description = "공유 수", example = "12")
        private Integer shareCount;

        private Double popularityScore;

        @Schema(description = "카테고리명", example = "피규어")
        private String categoryName;

        @Schema(description = "상품 유형", example = "SALE")
        private String productType;

        @Schema(description = "반려 사유", example = "안돼요")
        private String rejectReason;

        @Schema(description = "대표 이미지")
        private AttachmentFileResponse thumbnailDto;

        @Schema(description = "상품 출시일", example = "2024-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate releaseDate;

        @Schema(description = "상품 등록일", example = "2024-01-10T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "피드용 상품 응답")
    public static class FeedProduct {
        @Schema(description = "상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "건담 로봇")
        private String name;

        @Schema(description = "브랜드명", example = "반다이")
        private String brand;

        @Schema(description = "상품 정가", example = "25000")
        private String price;

        @Schema(description = "상품 상태", example = "APPROVED")
        private ProductStatus productStatus;

        @Schema(description = "대표 이미지")
        private AttachmentFileResponse thumbnailDto;

        public static FeedProduct of(Product product) {
            return FeedProduct.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .price(product.getPrice())
                .thumbnailDto(
                    AttachmentFileResponse.of(product.getProductAttachmentFiles().get(0)))
                .productStatus(product.getStatus())
                .build();
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "배틀 아이템 상품 응답")
    public static class BattleItemProduct {
        @Schema(description = "상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "건담 로봇")
        private String name;

        @Schema(description = "브랜드명", example = "반다이")
        private String brand;

        @Schema(description = "상품 정가", example = "25000")
        private String price;

        @Schema(description = "상품 상태", example = "APPROVED")
        private ProductStatus productStatus;

        @Schema(description = "대표 이미지")
        private AttachmentFileResponse thumbnailDto;

        public static BattleItemProduct of(Product product) {
            return BattleItemProduct.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .price(product.getPrice())
                .productStatus(product.getStatus())
                .thumbnailDto(
                    AttachmentFileResponse.of(product.getProductAttachmentFiles().get(0)))
                .build();
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 상세 응답")
    public static class ProductDetail {
        @Schema(description = "상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "건담 로봇")
        private String name;

        @Schema(description = "브랜드명", example = "반다이")
        private String brand;

        @Schema(description = "상품 상태", example = "APPROVED")
        private ProductStatus status;

        @Schema(description = "상품 상세 설명", example = "1/144 스케일의 정밀한 건담 프라모델")
        private String description;

        @Schema(description = "판매처 정보", example = "인터넷 쇼핑몰, 완구점")
        private String distributor;

        @Schema(description = "상품 정가", example = "25000")
        private String price;

        @Schema(description = "상품 주요 특징", example = "리미티드 에디션, 특별 컬러링")
        private String feature;

        @Schema(description = "상품 태그", example = "건담;프라모델;한정판;콜렉션")
        private List<String> tags;

        @Schema(description = "조회수", example = "150")
        private Integer viewCount;

        @Schema(description = "찜하기 수", example = "23")
        private Integer likeCount;

        @Schema(description = "평균 평점", example = "4.5")
        private Double averageRating;

        @Schema(description = "리뷰 수", example = "12")
        private Integer reviewCount;

        @Schema(description = "상품 등록자 ID", example = "100")
        private Long creatorId;

        @Schema(description = "상품 출시일", example = "2024-01-15")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate releaseDate;

        @Schema(description = "상품 등록일", example = "2024-01-10T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(description = "상품 수정일", example = "2024-01-12T15:20:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        @Schema(description = "서브 카테고리 상세 dto")
        private CategoryResponse.SubCategoryDetail subCategoryDetail;

        @Schema(description = "상품 유형", example = "SALE")
        private String productType;

        @Schema(description = "상품 첨부파일 목록")
        private List<AttachmentFileResponse> attachmentFiles;

        @Schema(description = "사용자의 반응 목록", example = "false")
        private ProductReactionResponse.ProductUserReaction userReaction;

        @Schema(description = "활성 리뷰 목록")
        private List<ProductReviewResponse.ReviewList> reviews;

        public static ProductDetail of(Product product, ProductUserReaction userReaction,
            List<ProductReviewResponse.ReviewList> reviews) {

            return ProductDetail.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .status(product.getStatus())
                .description(product.getDescription())
                .distributor(product.getDistributor())
                .price(product.getPrice())
                .feature(product.getFeature())
                .tags(parseToList(product.getTags()))
                .viewCount(product.getViewCount())
                .likeCount(product.getLikeCount())
                .averageRating(product.getAvgRating())
                .reviewCount(reviews.size())
                .creatorId(product.getCreatorId())
                .releaseDate(product.getReleaseDate())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .subCategoryDetail(CategoryResponse.SubCategoryDetail.from(product.getCategory()))
                .attachmentFiles(product.getProductAttachmentFiles()
                    .stream()
                    .map(AttachmentFileResponse::of)
                    .toList())
                .userReaction(userReaction)
                .reviews(reviews)
                .build();
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 피드 응답")
    public static class ProductFeed {
        @Schema(description = "상품 ID", example = "1")
        private Long productId;

        @Schema(description = "피드 ID (커서로 사용)", example = "100")
        private Long feedId;

        @Schema(description = "피드 제목", example = "오늘의 추천 상품")
        private String feedTitle;

        @Schema(description = "피드 설명", example = "이 상품 정말 좋아요!")
        private String description;

        @Schema(description = "작성자 정보")
        private ExternalUserInfo userInfo;

        @Schema(description = "피드 썸네일 이미지")
        private AttachmentFileResponse feedThumbnail;

        @Schema(description = "좋아요 수", example = "42")
        private Integer likeCount;

        @Schema(description = "댓글 수", example = "7")
        private Integer commentCount;

        public static ProductFeed from(Feed feed, ExternalUserInfo userInfo) {
            return ProductFeed.builder()
                .feedId(feed.getId())
                .productId(feed.getProduct().getId())
                .feedTitle(
                    StringUtils.hasText(feed.getProduct().getName()) ? feed.getProduct().getName()
                        : feed.getProductNameCustom())
                .description(feed.getReview())
                .userInfo(userInfo)
                .feedThumbnail(
                    feed.getAttachmentFiles()
                        .stream()
                        .findFirst()
                        .map(AttachmentFileResponse::of)
                        .orElse(null)
                )
                .likeCount(feed.getLikeCount())
                .commentCount(feed.getCommentCount())
                .build();
        }

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 배틀 응답")
    public static class ProductBattle {
        @Schema(description = "상품 ID", example = "1")
        private Long productId;

        @Schema(description = "배틀 ID (커서로 사용)", example = "100")
        private Long battleId;

        @Schema(description = "배틀 제목", example = "오늘의 추천 상품")
        private String battleTitle;

        @Schema(description = "배틀 종료 일시", example = "2025-01-15T18:00:00")
        private LocalDateTime endDate;

        @Schema(description = "배틀 상태", example = "IN_PROGRESS")
        private BattleStatus status;

        @Schema(description = "배틀 참여 상품 목록")
        private List<ProductBattleItem> battleItems;

        public static ProductBattle from(Battle battle, List<BattleItem> items, Long productId) {
            List<ProductBattleItem> rankedItems = setRanking(
                items.stream()
                    .map(item -> ProductBattleItem.from(item, isCurrentProduct(item, productId)))
                    .collect(Collectors.toList())
            );

            // TOP 3 까지
            List<ProductBattleItem> battleItems = rankedItems.stream()
                .filter(item -> item.getRank() <= 3)
                .collect(Collectors.toList());

            // 현재 상품이 TOP 3에 없으면 추가
            rankedItems.stream()
                .filter(item -> item.getIsCurrentProduct() && item.getRank() > 3)
                .findFirst()
                .ifPresent(battleItems::add);

            return ProductBattle.builder()
                .productId(productId)
                .battleId(battle.getId())
                .battleTitle(battle.getTitle())
                .endDate(battle.getEndDate())
                .status(battle.getStatus())
                .battleItems(battleItems)
                .build();
        }

        private static boolean isCurrentProduct(BattleItem item, Long productId) {
            return item.getProduct() != null && item.getProduct().getId().equals(productId);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "배틀 참여 상품 정보")
    public static class ProductBattleItem implements Rankable {
        @Schema(description = "배틀 아이템 ID", example = "1")
        private Long battleItemId;

        @Schema(description = "상품명", example = "나이키 에어맥스 90")
        private String productName;

        private Integer rank;

        @Schema(description = "현재 조회 중인 상품 여부", example = "true")
        private Boolean isCurrentProduct;

        @Schema(description = "득표율 (%)", example = "33.07")
        private Double votePercentage;

        @Schema(description = "점수", example = "33")
        private Integer totalScore;


        public static ProductBattleItem from(BattleItem battleItem, Boolean isCurrentProduct) {
            return ProductBattleItem.builder()
                .battleItemId(battleItem.getId())
                .productName(battleItem.getProduct() != null ? battleItem.getProduct().getName()
                    : battleItem.getCustomName())
                .isCurrentProduct(isCurrentProduct)
                .votePercentage(battleItem.getBattle().getTotalScore() > 0
                    ? (double) battleItem.getTotalScore() / battleItem.getBattle().getTotalScore()
                    : 0.0)
                .totalScore(battleItem.getTotalScore())
                .build();
        }

        @Override
        public Long getId() {
            return this.battleItemId;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 생성 응답")
    public static class ProductCreate {
        @Schema(description = "생성된 상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "건담 로봇")
        private String name;

        @Schema(description = "상품 상태", example = "PENDING")
        private ProductStatus status;

        @Schema(description = "생성일", example = "2024-01-10T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(description = "생성 성공 메시지", example = "상품이 성공적으로 등록되었습니다. 관리자 승인 후 노출됩니다.")
        private String message;

        public static ProductCreate of(Product product) {
            return ProductCreate.builder()
                .id(product.getId())
                .name(product.getName())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .message("success")
                .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상품 수정 응답")
    public static class ProductUpdate {
        @Schema(description = "수정된 상품 ID", example = "1")
        private Long id;

        @Schema(description = "상품명", example = "건담 로봇")
        private String name;

        @Schema(description = "수정일", example = "2024-01-12T15:20:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        @Schema(description = "수정 성공 메시지", example = "상품 정보가 성공적으로 수정되었습니다.")
        private String message;

        public static ProductUpdate of(Product product) {
            return ProductUpdate.builder()
                .id(product.getId())
                .name(product.getName())
                .updatedAt(product.getUpdatedAt())
                .message("상품 정보가 성공적으로 수정되었습니다.")
                .build();
        }
    }
}
