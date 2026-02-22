package com.example.toycontent.app.carrier.controller.dto;


import com.example.toycontent.app.carrier.domain.CarrierItem;
import com.example.toycontent.app.product.domain.ProductAttachmentFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class CarrierItemResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "캐리어 아이템 상세")
    public static class Detail {

        @Schema(description = "아이템 ID", example = "1")
        private Long id;

        @Schema(description = "상품 ID", example = "10")
        private Long productId;

        @Schema(description = "상품명", example = "립스틱")
        private String productName;

        @Schema(description = "브랜드명", example = "롬앤")
        private String brandName;

        @Schema(description = "상품 이미지 URL", example = "https://example.com/image.jpg")
        private String productImageUrl;

        @Schema(description = "X 좌표", example = "0.5")
        private Double positionX;

        @Schema(description = "Y 좌표", example = "0.3")
        private Double positionY;

        @Schema(description = "Z 순서 (레이어 우선순위)", example = "1")
        private Integer zIndex;

        public static Detail from(CarrierItem item) {
            return Detail.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .brandName(item.getProduct().getBrand())
                    .productImageUrl(
                            item.getProduct().getProductAttachmentFiles().stream()
                                    .map(ProductAttachmentFile::getFileUrl)
                                    .findFirst()
                                    .orElse(null)
                    )
                    .positionX(item.getPositionX())
                    .positionY(item.getPositionY())
                    .zIndex(item.getZIndex())
                    .build();
        }
    }
}