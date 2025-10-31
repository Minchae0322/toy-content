package com.example.toycontent.app.battle.controller;


import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.service.BattleService;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BattleItemController", description = "배틀 아이템 API")
@RestController
@RequestMapping("/battles/{battleId}/items")
@RequiredArgsConstructor
public class BattleItemController {

  private final BattleService battleService;

  @Operation(summary = "배틀 아이템 추가")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> addBattleItems(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.AddBattleItems request) {
    battleService.requestAddBattleItems(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "아이템이 추가되었습니다."));
  }

  @Operation(summary = "배틀 아이템 제외 (생성자 전용)")
  @PatchMapping("/{itemId}/exclude")
  public ResponseEntity<ApiResponse<Void>> excludeBattleItem(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId) {
    battleService.excludeBattleItem(battleId, itemId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "아이템이 제외되었습니다."));
  }

  @Operation(summary = "배틀 아이템 승인 (검토 중 → 활성)")
  @PatchMapping("/{itemId}/approve")
  public ResponseEntity<ApiResponse<Void>> approveBattleItem(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId) {
    battleService.approveBattleItem(battleId, itemId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "아이템이 승인되었습니다."));
  }

  @Operation(summary = "배틀 아이템 신고")
  @PostMapping("/{itemId}/report")
  public ResponseEntity<ApiResponse<Void>> reportBattleItem(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.Report request) {
    battleService.reportBattleItem(battleId, itemId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "신고가 접수되었습니다."));
  }
}