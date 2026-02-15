package com.example.toycontent.app.battle.controller;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.Detail;
import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleHotList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.service.BattleItemCommentService;
import com.example.toycontent.app.battle.service.BattleService;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BattleController", description = "배틀 API")
@RestController
@RequestMapping("/battles")
@RequiredArgsConstructor
public class BattleController {

  private final BattleService battleService;
  private final BattleItemCommentService battleItemCommentService;

  @Operation(summary = "배틀 생성 권한 체크")
  @GetMapping("/creation/validation")
  public ResponseEntity<ApiResponse<Void>> validateCreation(
      @CurrentUserId Long userId) {
    battleService.validateCreation(userId);
    return ResponseEntity.ok(ApiResponse.success(null, "생성 가능합니다."));
  }

  @Operation(summary = "배틀 생성")
  @PostMapping
  public ResponseEntity<ApiResponse<BattleResponse.BattleCreateResponse>> createBattle(
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.CreateBattle request) {
    BattleResponse.BattleCreateResponse response = battleService.createBattle(userId, request);
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

  @Operation(summary = "배틀 핫 목록 조회")
  @GetMapping("/hot")
  public ResponseEntity<ApiResponse<List<BattleHotList>>> getHotBattleList() {
    List<BattleResponse.BattleHotList> response = battleService.getHotBattleList();
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "배틀 상세 조회")
  @GetMapping("/{battleId}")
  public ResponseEntity<ApiResponse<BattleResponse.BattleDetail>> getBattleDetail(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId(required = false) Long userId) {
    BattleResponse.BattleDetail response = battleService.getBattleDetail(battleId, userId);
    return ResponseEntity.ok(ApiResponse.success(response));
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
