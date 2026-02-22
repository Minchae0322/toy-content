package com.example.toycontent.app.carrier.controller;

import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.carrier.controller.dto.CarrierRequest;
import com.example.toycontent.app.carrier.controller.dto.CarrierResponse;
import com.example.toycontent.app.carrier.service.CarrierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CarrierController", description = "캐리어 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(value = "/carriers")
public class CarrierController {

    private final CarrierService carrierService;

    @Operation(summary = "내 캐리어 목록 조회", description = "사용자의 캐리어 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CarrierResponse.Summary>>> getMyCarriers(
            @CurrentUserId Long userId) {

        List<CarrierResponse.Summary> carriers = carrierService.getMyCarriers(userId);
        return ResponseEntity.ok(ApiResponse.success(carriers));
    }

    @Operation(summary = "캐리어 상세 조회", description = "캐리어의 아이템과 스티커를 포함한 상세 정보를 조회합니다.")
    @GetMapping("/{carrierId}")
    public ResponseEntity<ApiResponse<CarrierResponse.Detail>> getCarrier(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @CurrentUserId Long userId) {

        CarrierResponse.Detail carrier = carrierService.getCarrier(carrierId, userId);
        return ResponseEntity.ok(ApiResponse.success(carrier));
    }

    @Operation(summary = "캐리어 생성", description = "새로운 캐리어를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CarrierResponse.Summary>> createCarrier(
            @Valid @RequestBody CarrierRequest.CreateCarrier request,
            @CurrentUserId Long userId) {

        CarrierResponse.Summary carrier = carrierService.createCarrier(request, userId);
        return ResponseEntity.ok(ApiResponse.success(carrier, "캐리어가 생성되었습니다."));
    }


    @Operation(summary = "캐리어 스킨 변경", description = "캐리어의 스킨을 변경합니다.")
    @PatchMapping("/{carrierId}/skin")
    public ResponseEntity<ApiResponse<CarrierResponse.Summary>> updateCarrierSkin(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @Valid @RequestBody CarrierRequest.UpdateSkin request,
            @CurrentUserId Long userId) {

        CarrierResponse.Summary carrier = carrierService.updateCarrierSkin(carrierId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(carrier, "스킨이 변경되었습니다."));
    }

    @Operation(summary = "캐리어 삭제", description = "캐리어를 삭제합니다. 기본 캐리어는 삭제할 수 없습니다.")
    @DeleteMapping("/{carrierId}")
    public ResponseEntity<ApiResponse<Void>> deleteCarrier(
            @Parameter(description = "캐리어 ID") @PathVariable Long carrierId,
            @CurrentUserId Long userId) {

        carrierService.deleteCarrier(carrierId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "캐리어가 삭제되었습니다."));
    }
}