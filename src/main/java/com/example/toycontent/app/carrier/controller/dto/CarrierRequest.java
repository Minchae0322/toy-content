package com.example.toycontent.app.carrier.controller.dto;


import com.example.toycontent.app.common.enumuration.CarrierSkinType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CarrierRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캐리어 생성 요청")
    public static class CreateCarrier {

        @NotBlank(message = "캐리어 이름은 필수입니다.")
        @Size(max = 50, message = "캐리어 이름은 50자 이하여야 합니다.")
        @Schema(description = "캐리어 이름")
        private String name;

        @NotNull(message = "스킨 타입은 필수입니다.")
        @Schema(description = "스킨 타입", example = "DEFAULT", requiredMode = Schema.RequiredMode.REQUIRED)
        private CarrierSkinType skinType;

        @Schema(description = "스킨 색상 (#HEX)")
        private String skinColor;
    }


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캐리어 스킨 변경 요청")
    public static class UpdateSkin {

        @NotNull(message = "스킨 타입은 필수입니다.")
        @Schema(description = "스킨 타입", example = "DEFAULT", requiredMode = Schema.RequiredMode.REQUIRED)
        private CarrierSkinType skinType;

        @Schema(description = "스킨 색상 (#HEX)", example = "#FFFFFF")
        private String skinColor;
    }
}