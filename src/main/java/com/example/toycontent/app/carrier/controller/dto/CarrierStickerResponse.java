package com.example.toycontent.app.carrier.controller.dto;


import com.example.toycontent.app.carrier.domain.CarrierSticker;
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

        private Long id;
        private String stickerType;
        private Double positionX;
        private Double positionY;
        private Integer zIndex;
        private Double rotation;
        private Double scaleRatio;

        public static Detail from(CarrierSticker sticker) {
            return Detail.builder()
                    .id(sticker.getId())
                    .stickerType(sticker.getStickerType())
                    .positionX(sticker.getPositionX())
                    .positionY(sticker.getPositionY())
                    .zIndex(sticker.getZIndex())
                    .rotation(sticker.getRotation())
                    .scaleRatio(sticker.getScaleRatio())
                    .build();
        }
    }
}
