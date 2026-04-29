package com.example.toycontent.app.carrier.controller.dto;
import com.example.toycontent.app.common.enumuration.StickerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class CarrierStickerRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스티커 일괄 저장 요청 (캐리어의 최종 상태를 전달, 누락된 기존 스티커는 삭제됨)")
    public static class BulkSave {

        @Valid
        @NotNull(message = "스티커 목록은 필수입니다.")
        @Size(max = 20, message = "한 번에 최대 20개까지 저장할 수 있습니다.")
        @Schema(description = "캐리어에 남길 스티커 최종 상태 (빈 배열이면 전체 삭제)")
        private List<StickerUpsert> stickers;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "개별 스티커 upsert 데이터")
        public static class StickerUpsert {

            @Schema(description = "스티커 ID (null이면 신규 생성, 값이 있으면 기존 수정)", example = "15", nullable = true)
            private Long stickerId;

            @NotNull(message = "스티커 타입은 필수입니다.")
            @Schema(description = "스티커 타입", example = "TEXT", requiredMode = Schema.RequiredMode.REQUIRED)
            private StickerType stickerType;

            @Size(max = 200, message = "스티커 내용은 200자 이내로 입력해주세요.")
            @Schema(description = "스티커 텍스트 내용 (TEXT, PHOTO_TAG 타입에서 사용)", example = "맛있다!", nullable = true)
            private String content;

            @Schema(description = "스티커 이미지 URL (IMAGE 타입에서 사용)", example = "https://cdn.example.com/sticker.png", nullable = true)
            private String imageUrl;

            @DecimalMin(value = "0.0", message = "X 좌표는 0 이상이어야 합니다.")
            @DecimalMax(value = "1.0", message = "X 좌표는 1 이하여야 합니다.")
            @Schema(description = "X 좌표 (0.0 ~ 1.0 비율값)", example = "0.5")
            private Double positionX;

            @DecimalMin(value = "0.0", message = "Y 좌표는 0 이상이어야 합니다.")
            @DecimalMax(value = "1.0", message = "Y 좌표는 1 이하여야 합니다.")
            @Schema(description = "Y 좌표 (0.0 ~ 1.0 비율값)", example = "0.3")
            private Double positionY;

            @Schema(description = "z-index (겹침 순서, 클수록 위에 표시)", example = "1", nullable = true)
            private Integer zIndex;

            @DecimalMin(value = "-360.0", message = "회전 각도는 -360 이상이어야 합니다.")
            @DecimalMax(value = "360.0", message = "회전 각도는 360 이하여야 합니다.")
            @Schema(description = "회전 각도 (degree, 기본값 0)", example = "15.5", nullable = true)
            private Double rotation;

            @DecimalMin(value = "0.1", message = "스케일은 0.1 이상이어야 합니다.")
            @DecimalMax(value = "3.0", message = "스케일은 3.0 이하여야 합니다.")
            @Schema(description = "확대/축소 비율 (기본값 1.0)", example = "1.2", nullable = true)
            private Double scaleRatio;
        }
    }
}
