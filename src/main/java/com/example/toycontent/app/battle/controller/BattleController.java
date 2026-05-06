package com.example.toycontent.app.battle.controller;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.Detail;
import com.example.toycontent.app.battle.controller.dto.BattleReportRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleHotList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.service.BattleItemCommentService;
import com.example.toycontent.app.battle.service.BattleReportService;
import com.example.toycontent.app.battle.service.BattleService;
import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
import com.example.toycontent.app.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
  private final BattleReportService battleReportService;

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

  @Operation(summary = "배틀 수정",
      description = "부분 수정(PATCH). null 필드는 변경되지 않습니다. "
          + "title/description/thumbnail은 언제나 수정 가능하고, "
          + "카테고리·권한·기간·투표타입은 참여자가 0명일 때만 수정 가능합니다.")
  @PatchMapping("/{battleId}")
  public ResponseEntity<ApiResponse<Void>> updateBattle(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleRequest.UpdateBattle request) {
    battleService.updateBattle(userId, battleId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "배틀이 수정되었습니다."));
  }

  @Operation(summary = "배틀 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<Page<BattleResponse.BattleList>>> getBattles(
      @ParameterObject BattleSearchCondition condition,
      @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<BattleResponse.BattleList> response = battleService.getBattles(condition, pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "배틀 핫 목록 조회")
  @GetMapping("/hot")
  public ResponseEntity<ApiResponse<Page<BattleHotList>>> getHotBattleList(
      @ParameterObject @PageableDefault(size = 10, sort = "hotScore", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<BattleHotList> response = battleService.getHotBattleList(pageable);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(summary = "배틀 상세 조회")
  @GetMapping("/{battleId}")
  public ResponseEntity<ApiResponse<BattleResponse.BattleDetail>> getBattleDetail(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @CurrentUserId(required = false) Long userId,
      @CurrentUserIsAdmin boolean isAdmin) {

    BattleResponse.BattleDetail response = battleService.getBattleDetail(battleId, userId, isAdmin);
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

  @Operation(summary = "배틀 신고", description = "부적절한 배틀을 신고합니다. 동일 배틀은 한 번만 신고할 수 있습니다.")
  @PostMapping("/{battleId}/reports")
  public ResponseEntity<ApiResponse<Long>> reportBattle(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Valid @RequestBody BattleReportRequest request,
      @CurrentUserId Long userId) {
    Long reportId = battleReportService.report(battleId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(reportId, "신고가 접수되었습니다."));
  }
}
