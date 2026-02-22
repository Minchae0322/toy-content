package com.example.toycontent.app.carrier.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CarrierItemRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이템 추가 요청")
    public static class AddItem {

        @NotNull(message = "상품 ID는 필수입니다.")
        @Schema(description = "상품 ID")
        private Long productId;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "X 위치 비율 (0.0 ~ 1.0)")
        private Double positionX;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "Y 위치 비율 (0.0 ~ 1.0)")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이템 위치 변경 요청")
    public static class UpdatePosition {

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "X 위치 비율")
        private Double positionX;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "Y 위치 비율")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이템 위치 일괄 변경 요청")
    public static class UpdatePositionBulk {

        @NotNull
        @Schema(description = "아이템 ID")
        private Long itemId;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "X 위치 비율")
        private Double positionX;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "Y 위치 비율")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;
    }
}