package com.example.toycontent.app.battle.controller;

import com.example.toycontent.app.battle.controller.dto.BattleSwipeRequest;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.NextItems;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.Result;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.SwipeAck;
import com.example.toycontent.app.battle.service.BattleSwipeService;
import com.example.toycontent.app.common.annotation.CurrentVoterId;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.common.voter.VoterId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BattleSwipeController", description = "배틀 스와이프 API (VoteType.SWIPE 전용)")
@RestController
@RequestMapping("/battles/{battleId}/swipe")
@RequiredArgsConstructor
public class BattleSwipeController {

  private final BattleSwipeService battleSwipeService;

  @Operation(summary = "스와이프 1건 등록",
      description = "강추 PICK / PICK / PASS. 한 번 등록한 아이템은 변경 불가, 다시 노출되지 않음. 비로그인도 가능.")
  @PostMapping
  public ResponseEntity<ApiResponse<SwipeAck>> swipe(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentVoterId VoterId voter,
      @Valid @RequestBody BattleSwipeRequest.Swipe request) {
    SwipeAck ack = battleSwipeService.swipe(battleId, voter, request);
    return ResponseEntity.ok(ApiResponse.success(ack, "스와이프가 등록되었습니다."));
  }

  @Operation(summary = "다음 스와이프 아이템 조회",
      description = "voter가 아직 스와이프하지 않은 아이템 중 랜덤 N개. 중단 후 재진입 시에도 미진행분만 노출.")
  @GetMapping("/next")
  public ResponseEntity<ApiResponse<NextItems>> next(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentVoterId VoterId voter,
      @Parameter(description = "조회 개수 (기본 10)") @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(ApiResponse.success(
        battleSwipeService.findNextItems(battleId, voter, size),
        "다음 아이템을 조회했습니다."));
  }

  @Operation(summary = "스와이프 결과(랭킹) 조회",
      description = "활성 아이템 전체 대상, 단순 합산(STRONG_PICK*3 + PICK*1) 내림차순.")
  @GetMapping("/result")
  public ResponseEntity<ApiResponse<Result>> result(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId) {
    return ResponseEntity.ok(ApiResponse.success(
        battleSwipeService.getResult(battleId),
        "스와이프 결과를 조회했습니다."));
  }
}
