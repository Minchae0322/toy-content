package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.common.enumuration.OneMouthStatus;
import com.example.toycontent.app.common.enumuration.Unit;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
@Data
public abstract class OneMouthResponse {

    //TODO - 재정리 필요
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductTradeSummary {

        private Long id;                      // 거래 ID
        private String title;                 // 거래 제목
        private Long sellingPrice;            // 판매가
        private OneMouthStatus status;        // 거래 상태
        private String thumbnailUrl;          // 대표 이미지 URL
        private String sellerNickname;        // 판매자 닉네임
        private Double sellerRating;          // 판매자 평점
        private String tradeLocation;         // 거래 지역
        private Double distance;              // 현재 위치와 거리 (옵션)
        private LocalDateTime createdAt;      // 등록일

        /**
         * 엔티티 → DTO 변환 메서드
         * @param entity OneMouth 엔티티
         */
        public static ProductTradeSummary of(SalePost entity) {

            return ProductTradeSummary.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .sellingPrice(entity.getPrice())
                .status(entity.getStatus())
                .thumbnailUrl(entity.getAttachmentFiles().get(0).getFileUrl())
                .sellerNickname(entity.getSellerName())
                .tradeLocation(entity.getTradeLocation())
                .createdAt(entity.getCreatedAt())
                .build();
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListView {
        @Schema(title = "게시글 기본 키 ID", example = "1")
        private Long oneMouthId;

        @Schema(title = "게시글 제목", example = "스타벅스 아메리카노 Tall 쿠폰 1매")
        private String title;

        @Schema(title = "판매 상태", description = "ON_SALE(판매중), RESERVED(예약중), SOLD_OUT(판매완료)")
        private OneMouthStatus oneMouthStatus;

        @Schema(title = "제품 상세 설명", example = "선물 받았는데 커피를 안 마셔서 판매합니다.")
        private String description;

        @Schema(title = "판매 가격", example = "7500")
        private Long sellingPrice;

        @Schema(title = "판매자 아이디", example = "123")
        private Long sellerId;

        @Schema(title = "판매자 이름", example = "카페인중독자")
        private String sellerName;

        @Schema(title = "연결된 공식 제품 ID", example = "1")
        private Long productId;

        @Schema(title = "연결된 공식 제품명", example = "스타벅스 아메리카노 Tall")
        private String productName;

        @Schema(title = "카테고리 아이디", example = "1")
        private Long categoryId;

        @Schema(title = "카테고리명", example = "편의점")
        private String categoryName;

        @Schema(title = "거래 희망 지역", example = "강남역 2번 출구")
        private String tradeLocation;

        @Schema(title = "생성 시간")
        private LocalDateTime createdAt;

        @Schema(title = "조회수", example = "42")
        private Integer viewCount;

        @Schema(title = "관심 등록 수", example = "15")
        private Integer favoriteCount;

        @Schema(title = "채팅방 수", example = "3")
        private Integer chatRoomCount;

        @Schema(title = "썸네일 URL", example = "https://example.com/images/thumbnail.jpg")
        private String thumbnailUrl;

    }




    @Data
    @Builder
    public static class Detail {
        @Schema(title = "게시글 기본 키 ID", description = "게시글의 고유 식별자", example = "1")
        private Long oneMouthId;

        @Schema(title = "게시글 제목", description = "사용자가 작성한 게시글 제목", example = "맛있는 초콜릿 한입거리")
        private String title;

        @Schema(title = "사용자 설정 수량 값", description = "판매자가 설정한 수량", example = "5")
        private String quantity;

        @Schema(title = "유닛 (개, 입, 그램, 커스텀)", description = "수량의 단위", example = "PIECE")
        private Unit unit;

        @Schema(title = "가격", description = "상품 가격 (원 단위)", example = "3000")
        private Long price;

        @Schema(title = "판매 상태", description = "현재 상품의 판매 상태", example = "FOR_SALE")
        private OneMouthStatus oneMouthStatus;

        @Schema(title = "제품 상세 설명", description = "상품에 대한 자세한 설명")
        private String description;

        @Schema(title = "판매자 아이디", description = "상품을 등록한 판매자의 ID", example = "123")
        private Long sellerId;

        @Schema(title = "판매자 거래 위치", description = "거래 희망 위치", example = "강남구 역삼동")
        private String location;

        @Schema(title = "생성 시간", description = "게시글 작성 시간")
        private LocalDateTime createdAt;

        @Schema(title = "수정 시간", description = "게시글 마지막 수정 시간")
        private LocalDateTime updatedAt;

        @Schema(title = "제품 유형", description = "상품 거래 유형", example = "sale")
        private String productType;

        @Schema(title = "조회수", description = "게시글 조회 횟수", example = "150")
        private Integer hits;

        @Schema(title = "관심수", description = "사용자들이 관심 표시한 횟수", example = "25")
        private Integer favoritesCount;

        @Schema(title = "썸네일 이미지 URL", description = "대표 이미지 URL")
        private String thumbnailUrl;

        @Schema(title = "현재 사용자 관심 여부", description = "로그인한 사용자가 이 상품에 관심을 표시했는지 여부")
        private Boolean isUserFavorited;


        @Schema(title = "첨부파일 리스트", description = "상품 이미지 및 첨부파일 목록")
        private List<AttachmentFileResponse> oneMouthAttachmentFiles;

        public static Detail from(SalePost salePost, Boolean isUserFavorited) {
            return Detail.builder()
                .oneMouthId(salePost.getId())
                .title(salePost.getTitle())
                .build();
        }

    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "판매 게시글 생성 응답")
    public static class SalePostCreateResponse {

        @Schema(description = "생성된 게시글 ID", example = "1")
        private Long salePostId;

        public static SalePostCreateResponse of(SalePost salePost) {
            return SalePostCreateResponse.builder()
                .salePostId(salePost.getId())
                .build();
        }
    }



}
