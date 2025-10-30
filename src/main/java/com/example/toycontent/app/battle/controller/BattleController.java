package com.example.toycontent.app.battle.controller;


import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.service.BattleService;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "BattleController", description = "배틀 API")
@RestController
@RequestMapping("/battles")
@RequiredArgsConstructor
public class BattleController {

  private final BattleService battleService;

  @Operation(summary = "배틀 생성 권한 체크")
  @GetMapping("/creation/validation")
  public ResponseEntity<ApiResponse<Void>> validateCreation(@CurrentUserId Long userId) {
    battleService.validateCreation(userId);
    return ResponseEntity.ok(ApiResponse.success(null, "생성 가능합니다."));
  }

  @Operation(summary = "배틀 생성")
  @PostMapping
  public ResponseEntity<ApiResponse<BattleResponse.BattleDetail>> createBattle(
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.CreateBattle request) {
    BattleResponse.BattleDetail response = battleService.createBattle(userId, request);
    return ResponseEntity.ok(ApiResponse.success(response, "배틀이 생성되었습니다."));
  }

  @Operation(summary = "배틀 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<Page<BattleResponse.BattleList>>> getBattles(
      @ParameterObject BattleSearchCondition condition,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    Page<BattleResponse.BattleList> response = battleService.getBattles(condition, pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "배틀 상세 조회")
  @GetMapping("/{battleId}")
  public ResponseEntity<ApiResponse<BattleResponse.BattleDetail>> getBattleDetail(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId) {
    BattleResponse.BattleDetail response = battleService.getBattleDetail(battleId, userId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "내가 생성한 배틀 목록")
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<Page<BattleResponse.BattleList>>> getMyBattles(
      @CurrentUserId Long userId,
      @Parameter(description = "배틀 상태") @RequestParam(required = false) String status,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    Page<BattleResponse.BattleList> response = battleService.getMyBattles(userId, status, pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "배틀 통계 조회 (생성자 전용)")
  @GetMapping("/{battleId}/statistics")
  public ResponseEntity<ApiResponse<BattleResponse.Statistics>> getBattleStatistics(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId) {
    BattleResponse.Statistics response = battleService.getBattleStatistics(battleId, userId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "배틀 투표")
  @PostMapping("/{battleId}/vote")
  public ResponseEntity<ApiResponse<Void>> vote(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.Vote request) {
    battleService.vote(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "투표가 완료되었습니다."));
  }

  @Operation(summary = "배틀 투표 취소")
  @DeleteMapping("/{battleId}/vote")
  public ResponseEntity<ApiResponse<Void>> cancelVote(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId) {
    battleService.cancelVote(battleId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "투표가 취소되었습니다."));
  }

  @Operation(summary = "배틀 공지 등록 (생성자 전용)")
  @PostMapping("/{battleId}/notice")
  public ResponseEntity<ApiResponse<Void>> addNotice(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.AddNotice request) {
    battleService.addNotice(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "공지가 등록되었습니다."));
  }

  @Operation(summary = "배틀 조기 종료 (생성자 전용)")
  @PatchMapping("/{battleId}/close")
  public ResponseEntity<ApiResponse<Void>> closeBattle(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.CloseBattle request) {
    battleService.closeBattle(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "배틀이 종료되었습니다."));
  }
}
