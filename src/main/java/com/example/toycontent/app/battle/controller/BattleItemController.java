package com.example.toycontent.app.battle.controller;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.Detail;
import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleVoteRequest;
import com.example.toycontent.app.battle.service.BattleItemCommentService;
import com.example.toycontent.app.battle.service.BattleItemService;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BattleItemController", description = "배틀 아이템 API")
@RestController
@RequestMapping("/battles/{battleId}/items")
@RequiredArgsConstructor
public class BattleItemController {

  private final BattleItemService battleItemService;
  private final BattleItemCommentService battleItemCommentService;

  @Operation(summary = "배틀 아이템 추가")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> addBattleItems(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.AddBattleItems request) {
    battleItemService.addBattleItems(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "아이템이 추가되었습니다."));
  }

  @Operation(summary = "배틀 아이템 승인 (검토 중 → 활성)")
  @PatchMapping("/{itemId}/approve")
  public ResponseEntity<ApiResponse<Void>> approveBattleItem(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId) {
    battleItemService.approveBattleItem(battleId, itemId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "아이템이 승인되었습니다."));
  }

  @Operation(summary = "배틀 아이템 제외 (생성자 전용)")
  @PatchMapping("/{itemId}/exclude")
  public ResponseEntity<ApiResponse<Void>> excludeBattleItem(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId) {
    battleItemService.excludeBattleItem(battleId, itemId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "아이템이 제외되었습니다."));
  }

  @Operation(summary = "배틀 아이템 투표")
  @PostMapping("/vote")
  public ResponseEntity<ApiResponse<Void>> vote(
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleVoteRequest.Vote request) {
    battleItemService.vote(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "투표가 완료되었습니다."));
  }

  @Operation(summary = "배틀 아이템 투표 취소")
  @DeleteMapping("/{battleItemId}/vote")
  @Deprecated
  public ResponseEntity<ApiResponse<Void>> cancelVote(
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long battleItemId,
      @CurrentUserId Long userId) {
    battleItemService.cancelVote(battleItemId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "투표가 취소되었습니다."));
  }


  @Operation(summary = "배틀 아이템 신고")
  @PostMapping("/{itemId}/report")
  public ResponseEntity<ApiResponse<Void>> reportBattleItem(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.Report request) {
    battleItemService.reportBattleItem(battleId, itemId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "신고가 접수되었습니다."));
  }

  @Operation(summary = "배틀 전체 코멘트 조회", description = "정렬: ?sort=likeCount,desc (공감순) / ?sort=createdAt,desc (최신순)")
  @GetMapping("/{battleId}/comments")
  public ResponseEntity<ApiResponse<Slice<Detail>>> getBattleComments(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId(required = false) Long userId,
      @PageableDefault(size = 10, sort = "likeCount", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.success(
        battleItemCommentService.getBattleComments(battleId, userId, pageable)));
  }
}