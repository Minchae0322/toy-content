package com.example.toycontent.app.carrier.controller.dto;

import com.example.toycontent.app.common.enumuration.StickerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
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

        @NotNull(message = "스티커 타입은 필수입니다.")
        @Schema(description = "스티커 타입")
        private StickerType stickerType;

        @Size(max = 100)
        @Schema(description = "이모지 또는 캡션")
        private String content;

        @Size(max = 500)
        @Schema(description = "이미지 URL (PHOTO_TAG일 때 사용)")
        private String imageUrl;

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

        @Size(max = 100)
        @Schema(description = "이모지 또는 캡션")
        private String content;

        @Size(max = 500)
        @Schema(description = "이미지 URL (PHOTO_TAG일 때 사용)")
        private String imageUrl;
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

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemoveBulk {
        @NotEmpty(message = "삭제할 스티커 ID를 입력해주세요.")
        @Size(max = 50, message = "한 번에 최대 50개까지 삭제할 수 있습니다.")
        private List<Long> stickerIds;
    }
}
