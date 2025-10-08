package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.common.enumuration.OneMouthStatus;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.enumuration.Unit;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;



@RequiredArgsConstructor
@Data
public abstract class OneMouthResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListView {
        @Schema(title = "게시글 기본 키 ID")
        private Long oneMouthId;

        @Schema(title = "게시글 제목")
        private String title;

        @Schema(title = "사용자 설정 수량 값")
        private String quantity;

        @Schema(title = "유닛 (개, 입, 그램, 커스텀)")
        private Unit unit;

        @Schema(title = "가격")
        private Long price;

        @Schema(title = "판매 상태 (판매중, 품절, 예약중, 판매중단)")
        private ProductStatus productStatus;

        @Schema(title = "제품 상세 설명")
        private String description;

        @Schema(title = "판매자 아이디")
        private Long sellerId;

        @Schema(title = "카테고리 아이디")
        private Long categoryId;

        @Schema(title = "카테고리명")
        private String categoryName;

        @Schema(title = "판매자 거래 위치")
        private String location;

        @Schema(title = "생성 시간")
        private LocalDateTime createdAt;

        @Schema(title = "수정 시간")
        private LocalDateTime updatedAt;

        @Schema(title = "제품 유형 (예: rental, sale)")
        private String productType;

        @Schema(title = "조회수")
        private Integer hits;

        @Schema(title = "관심수")
        private Integer favoritesCount;

        @Schema(title = "썸네일 URL")
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

        public static Detail from(OneMouth oneMouth, Boolean isUserFavorited) {
            return Detail.builder()
                .oneMouthId(oneMouth.getId())
                .title(oneMouth.getTitle())
                .build();
        }

    }
}
