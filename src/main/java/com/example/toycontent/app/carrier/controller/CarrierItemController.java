package com.example.toycontent.app.carrier.controller;


import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.carrier.controller.dto.CarrierItemRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierItemResponse;
import com.example.toycontent.app.carrier.controller.dto.CarrierStickerRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierStickerResponse;
import com.example.toycontent.app.carrier.service.CarrierItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CarrierItemController", description = "캐리어 아이템 & 스티커 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(value = "/carriers/{carrierId}")
public class CarrierItemController {

    private final CarrierItemService carrierItemService;

    // ===== 아이템 =====

    @Operation(summary = "아이템 담기", description = "캐리어에 상품을 추가합니다.")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CarrierItemResponse.Detail>> addItem(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Valid @RequestBody CarrierItemRequest.AddItem request,
            @CurrentUserId Long userId) {

        CarrierItemResponse.Detail item = carrierItemService.addItem(carrierId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(item, "캐리어에 담았습니다."));
    }

    @Operation(summary = "아이템 위치 변경", description = "캐리어 내 아이템의 위치를 변경합니다.")
    @PatchMapping("/items/{itemId}/position")
    public ResponseEntity<ApiResponse<CarrierItemResponse.Detail>> updateItemPosition(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Parameter(description = "아이템 ID") @PathVariable Long itemId,
            @Valid @RequestBody CarrierItemRequest.UpdatePosition request,
            @CurrentUserId Long userId) {

        CarrierItemResponse.Detail item = carrierItemService.updateItemPosition(carrierId, itemId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(item, "위치가 변경되었습니다."));
    }

    @Operation(summary = "아이템 위치 일괄 저장", description = "여러 아이템의 위치를 한번에 저장합니다.")
    @PutMapping("/items/positions")
    public ResponseEntity<ApiResponse<List<CarrierItemResponse.Detail>>> updateItemPositions(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Valid @RequestBody List<CarrierItemRequest.UpdatePositionBulk> request,
            @CurrentUserId Long userId) {

        List<CarrierItemResponse.Detail> items = carrierItemService.updateItemPositions(carrierId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(items, "위치가 저장되었습니다."));
    }

    @Operation(summary = "아이템 제거", description = "캐리어에서 아이템을 제거합니다.")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Parameter(description = "아이템 ID") @PathVariable Long itemId,
            @CurrentUserId Long userId) {

        carrierItemService.removeItem(carrierId, itemId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "캐리어에서 제거되었습니다."));
    }

    // ===== 스티커 =====

    @Operation(summary = "스티커 붙이기", description = "캐리어에 스티커를 추가합니다.")
    @PostMapping("/stickers")
    public ResponseEntity<ApiResponse<CarrierStickerResponse.Detail>> addSticker(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Valid @RequestBody CarrierStickerRequest.AddSticker request,
            @CurrentUserId Long userId) {

        CarrierStickerResponse.Detail sticker = carrierItemService.addSticker(carrierId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(sticker, "스티커를 붙였습니다."));
    }

    @Operation(summary = "스티커 위치/변환 변경", description = "스티커의 위치, 회전, 크기를 변경합니다.")
    @PatchMapping("/stickers/{stickerId}")
    public ResponseEntity<ApiResponse<CarrierStickerResponse.Detail>> updateSticker(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Parameter(description = "스티커 ID") @PathVariable Long stickerId,
            @Valid @RequestBody CarrierStickerRequest.UpdateSticker request,
            @CurrentUserId Long userId) {

        CarrierStickerResponse.Detail sticker = carrierItemService.updateSticker(carrierId, stickerId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(sticker, "스티커가 변경되었습니다."));
    }

    @Operation(summary = "스티커 일괄 저장", description = "여러 스티커의 위치를 한번에 저장합니다.")
    @PutMapping("/stickers/positions")
    public ResponseEntity<ApiResponse<List<CarrierStickerResponse.Detail>>> updateStickerPositions(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Valid @RequestBody List<CarrierStickerRequest.UpdateStickerBulk> request,
            @CurrentUserId Long userId) {

        List<CarrierStickerResponse.Detail> stickers = carrierItemService.updateStickerPositions(carrierId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(stickers, "스티커 위치가 저장되었습니다."));
    }

    @Operation(summary = "스티커 제거", description = "캐리어에서 스티커를 제거합니다.")
    @DeleteMapping("/stickers/{stickerId}")
    public ResponseEntity<ApiResponse<Void>> removeSticker(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Parameter(description = "스티커 ID") @PathVariable Long stickerId,
            @CurrentUserId Long userId) {

        carrierItemService.removeSticker(carrierId, stickerId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "스티커를 제거했습니다."));
    }
}
