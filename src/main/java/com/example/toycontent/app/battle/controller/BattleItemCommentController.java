package com.example.toycontent.app.battle.controller;


import com.example.toycontent.app.battle.controller.dto.BattleItemCommentRequest;
import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse;
import com.example.toycontent.app.battle.service.BattleItemCommentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BattleItemCommentController", description = "배틀 코멘트 API")
@RestController
@RequestMapping("/battles/{battleId}/items/{itemId}/comments")
@RequiredArgsConstructor
public class BattleItemCommentController {

  private final BattleItemCommentService commentService;

  @Operation(summary = "코멘트 작성")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> createComment(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long itemId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleItemCommentRequest.Create request) {

    commentService.createComment(battleId, itemId, userId, request);

    return ResponseEntity.ok(ApiResponse.success(null, "코멘트가 등록되었습니다."));
  }

  @Operation(summary = "코멘트 목록 조회", description = "정렬: ?sort=likeCount,desc (공감순) / ?sort=createdAt,desc (최신순)")
  @GetMapping
  public ResponseEntity<ApiResponse<Slice<BattleItemCommentResponse.Detail>>> getComments(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long itemId,
      @CurrentUserId(required = false) Long userId,
      @PageableDefault(size = 10, sort = "likeCount", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(ApiResponse.success(
        commentService.getComments(battleId, itemId, userId, pageable)));
  }


  @Operation(summary = "코멘트 수정")
  @PutMapping("/{commentId}")
  public ResponseEntity<ApiResponse<Void>> updateComment(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long itemId,
      @Parameter(description = "코멘트 ID") @PathVariable Long commentId,
      @CurrentUserId Long userId,
      @Valid @RequestBody BattleItemCommentRequest.Update request) {
    commentService.updateComment(commentId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "코멘트가 수정되었습니다."));
  }

  @Operation(summary = "코멘트 삭제")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long itemId,
      @Parameter(description = "코멘트 ID") @PathVariable Long commentId,
      @CurrentUserId Long userId) {
    commentService.deleteComment(commentId, userId);
    return ResponseEntity.ok(ApiResponse.success(null, "코멘트가 삭제되었습니다."));
  }

  @Operation(summary = "코멘트 공감 토글")
  @PostMapping("/{commentId}/like")
  public ResponseEntity<ApiResponse<BattleItemCommentResponse.LikeResult>> toggleLike(
      @Parameter(description = "배틀 ID") @PathVariable Long battleId,
      @Parameter(description = "배틀 아이템 ID") @PathVariable Long itemId,
      @Parameter(description = "코멘트 ID") @PathVariable Long commentId,
      @CurrentUserId Long userId) {
    return ResponseEntity.ok(ApiResponse.success(
        commentService.toggleLike(commentId, userId)));
  }
}
