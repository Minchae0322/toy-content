package com.example.toycontent.app.product.controller.dto;


import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.utils.TagParsingUtil;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

public abstract class ProductRequest {

    /**
     * 제품 등록 요청 DTO
     * 새로운 제품을 등록할 때 사용되는 데이터 전송 객체
     */
    @Data
    @Builder
    @Schema(description = "제품 등록 요청")
    public static class ProductCreate {

        @NotBlank(message = "제품명은 필수입니다.")
        @Size(min = 2, max = 100, message = "제품명은 2자 이상 100자 이하로 입력해주세요.")
        @Schema(description = "제품명", example = "스타벅스 콜드브루 커피")
        private String name;

        @NotBlank(message = "브랜드명은 필수입니다.")
        @Size(min = 1, max = 100, message = "브랜드명은 1자 이상 100자 이하로 입력해주세요.")
        @Schema(description = "브랜드명 (제조사 또는 판매업체)", example = "스타벅스")
        private String brand;

        @Size(max = 1000, message = "제품 설명은 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 상세 설명 (맛, 특징, 용량, 성분 등)", example = "진한 콜드브루 원액에 부드러운 우유를 더한 시그니처 음료")
        private String description;

        @Size(max = 1000, message = "판매처 정보는 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 판매처 (구매 가능한 장소)", example = "스타벅스 매장, 편의점, 대형마트")
        private String distributor;

        @NotNull(message = "상품 가격은 필수 입니다.")
        @Schema(description = "제품 정가 (원 단위)", example = "4500")
        private String price;

        @Size(max = 1000, message = "제품 특징은 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 주요 특징 (세미콜론으로 구분)", example = "신제품;한정판매;프리미엄")
        private String feature;

        @Size(max = 1000, message = "태그는 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 태그 (검색용 키워드, 세미콜론으로 구분)", example = "커피;콜드브루;시원한;부드러운")
        private List<String> tags;

        @Schema(description = "제품 출시일 (브랜드 공식 출시일)", example = "2024-01-15")
        private LocalDate releaseDate;

        @NotNull(message = "카테고리는 필수입니다.")
        @Positive(message = "올바른 카테고리를 선택해주세요.")
        @Schema(description = "제품 카테고리 ID", example = "1")
        private Long categoryId;

        @NotNull(message = "대표이미지는 필수 입니다.")
        @Schema(description = "대표이미지")
        private AttachmentInfo thumbnailAttachmentInfo;

        @Schema(description = "첨부파일 정보 목록")
        private List<AttachmentInfo> attachmentFileInfos;

        public Product toEntity(Category category, Long creatorId) {
            return Product.builder()
                .name(name)
                .brand(brand)
                .description(description)
                .distributor(distributor)
                .price(price)
                .feature(feature)
                .status(ProductStatus.PENDING)
                .tags(TagParsingUtil.listToString(tags))
                .releaseDate(releaseDate)
                .category(category)
                .creatorId(creatorId)
                .build();
        }

    }

    @Data
    @Builder
    @Schema(description = "제품 상태 변경 요청")
    public static class ProductStatusRequest {

        @NotNull(message = "상태는 필수입니다")
        @Schema(description = "변경할 상태", example = "ACTIVE")
        private ProductStatus status;

        @Schema(description = "반려 사유")
        private String rejectReason;
    }

    /**
     * 제품 수정 요청 DTO
     * 기존 제품 정보를 수정할 때 사용되는 데이터 전송 객체
     */
    @Data
    @Builder
    @Schema(description = "제품 수정 요청")
    public static class ProductUpdate {

        @Size(min = 2, max = 100, message = "제품명은 2자 이상 100자 이하로 입력해주세요.")
        @Schema(description = "제품명", example = "스타벅스 콜드브루 커피")
        private String name;

        @Size(min = 1, max = 100, message = "브랜드명은 1자 이상 100자 이하로 입력해주세요.")
        @Schema(description = "브랜드명", example = "스타벅스")
        private String brand;

        @Size(max = 1000, message = "제품 설명은 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 상세 설명")
        private String description;

        @Size(max = 1000, message = "판매처 정보는 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 판매처")
        private String distributor;

        @Pattern(regexp = "^[0-9]{1,10}$", message = "가격은 숫자만 입력 가능하며 최대 10자리까지 입력할 수 있습니다.")
        @Schema(description = "제품 정가 (원 단위)")
        private String price;

        @Size(max = 1000, message = "제품 특징은 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 주요 특징")
        private String feature;

        @Size(max = 1000, message = "태그는 1000자 이하로 입력해주세요.")
        @Schema(description = "제품 태그")
        private String tags;

        @PastOrPresent(message = "출시일은 현재 날짜보다 이후일 수 없습니다.")
        @Schema(description = "제품 출시일")
        private LocalDate releaseDate;

        @Positive(message = "올바른 카테고리를 선택해주세요.")
        @Schema(description = "제품 카테고리 ID")
        private Long categoryId;
    }


}
