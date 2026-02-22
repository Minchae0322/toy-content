package com.example.toycontent.app.carrier.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CarrierStickerRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스티커 추가 요청")
    public static class AddSticker {

        @NotBlank(message = "스티커 타입은 필수입니다.")
        @Schema(description = "스티커 타입 (STAR, HEART, FIRE 등)")
        private String stickerType;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "X 위치 비율")
        private Double positionX;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "Y 위치 비율")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;

        @Schema(description = "회전 각도 (0 ~ 360)", defaultValue = "0.0")
        private Double rotation;

        @Schema(description = "크기 비율 (1.0 = 기본)", defaultValue = "1.0")
        private Double scaleRatio;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스티커 변경 요청")
    public static class UpdateSticker {

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "X 위치 비율")
        private Double positionX;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "Y 위치 비율")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;

        @Schema(description = "회전 각도")
        private Double rotation;

        @Schema(description = "크기 비율")
        private Double scaleRatio;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스티커 일괄 변경 요청")
    public static class UpdateStickerBulk {

        @NotNull
        @Schema(description = "스티커 ID")
        private Long stickerId;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "X 위치 비율")
        private Double positionX;

        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        @Schema(description = "Y 위치 비율")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;

        @Schema(description = "회전 각도")
        private Double rotation;

        @Schema(description = "크기 비율")
        private Double scaleRatio;
    }
}
