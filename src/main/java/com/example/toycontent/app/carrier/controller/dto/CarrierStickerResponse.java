package com.example.toycontent.app.carrier.controller.dto;


import com.example.toycontent.app.carrier.domain.CarrierSticker;
import com.example.toycontent.app.common.enumuration.StickerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class CarrierStickerResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "캐리어 스티커 상세")
    public static class Detail {

        @Schema(description = "스티커 ID")
        private Long id;

        @Schema(description = "스티커 타입")
        private StickerType stickerType;

        @Schema(description = "이모지 또는 캡션")
        private String content;

        @Schema(description = "이미지 URL")
        private String imageUrl;

        @Schema(description = "X 위치 비율")
        private Double positionX;

        @Schema(description = "Y 위치 비율")
        private Double positionY;

        @Schema(description = "레이어 순서")
        private Integer zIndex;

        @Schema(description = "회전 각도")
        private Double rotation;

        @Schema(description = "크기 비율")
        private Double scaleRatio;

        public static Detail from(CarrierSticker sticker) {
            return Detail.builder()
                .id(sticker.getId())
                .stickerType(sticker.getStickerType())
                .content(sticker.getContent())
                .imageUrl(sticker.getImageUrl())
                .positionX(sticker.getPositionX())
                .positionY(sticker.getPositionY())
                .zIndex(sticker.getZIndex())
                .rotation(sticker.getRotation())
                .scaleRatio(sticker.getScaleRatio())
                .build();
        }
    }
}
