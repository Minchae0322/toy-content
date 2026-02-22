package com.example.toycontent.app.carrier.controller.dto;

import com.example.toycontent.app.carrier.domain.Carrier;
import com.example.toycontent.app.common.enumuration.CarrierSkinType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class CarrierResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "캐리어 요약")
    public static class Summary {

        @Schema(description = "캐리어 ID", example = "1")
        private Long id;

        @Schema(description = "스킨 타입", example = "DEFAULT")
        private CarrierSkinType skinType;

        @Schema(description = "스킨 색상 (#HEX)", example = "#FFFFFF")
        private String skinColor;

        @Schema(description = "기본 캐리어 여부", example = "true")
        private Boolean isDefault;

        @Schema(description = "아이템 수", example = "3")
        private Integer itemCount;

        @Schema(description = "스티커 수", example = "5")
        private Integer stickerCount;

        public static Summary from(Carrier carrier) {
            return Summary.builder()
                    .id(carrier.getId())
                    .skinType(carrier.getSkinType())
                    .skinColor(carrier.getSkinColor())
                    .isDefault(carrier.getIsDefault())
                    .itemCount(carrier.getItems().size())
                    .stickerCount(carrier.getStickers().size())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "캐리어 상세 (아이템 + 스티커 포함)")
    public static class Detail {

        @Schema(description = "캐리어 ID", example = "1")
        private Long id;

        @Schema(description = "캐리어 이름", example = "나의 캐리어")
        private String name;

        @Schema(description = "스킨 타입", example = "DEFAULT")
        private CarrierSkinType skinType;

        @Schema(description = "스킨 색상 (#HEX)", example = "#FFFFFF")
        private String skinColor;

        @Schema(description = "기본 캐리어 여부", example = "true")
        private Boolean isDefault;

        @Schema(description = "캐리어 아이템 목록")
        private List<CarrierItemResponse.Detail> items;

        @Schema(description = "캐리어 스티커 목록")
        private List<CarrierStickerResponse.Detail> stickers;

        public static Detail from(Carrier carrier) {
            return Detail.builder()
                    .id(carrier.getId())
                    .skinType(carrier.getSkinType())
                    .skinColor(carrier.getSkinColor())
                    .isDefault(carrier.getIsDefault())
                    .items(carrier.getItems().stream()
                            .map(CarrierItemResponse.Detail::from)
                            .toList())
                    .stickers(carrier.getStickers().stream()
                            .map(CarrierStickerResponse.Detail::from)
                            .toList())
                    .build();
        }
    }
}