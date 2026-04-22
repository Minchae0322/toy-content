package com.example.toycontent.app.feed.controller;

import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import com.example.toycontent.app.feed.service.FeedCommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feeds/{feedId}/comments")
public class FeedCommentController {

  private final FeedCommentService feedCommentService;

  @Operation(summary = "댓글 목록 조회", description = "피드의 댓글 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<Page<CommentItem>>> getComments(
      @PathVariable Long feedId,
      @ParameterObject @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

    Page<CommentItem> commentList =
        feedCommentService.getComments(feedId, pageable);

    return ResponseEntity.ok(ApiResponse.success(commentList));
  }

  @Operation(summary = "댓글/답글 생성",
      description = "피드에 댓글을 생성합니다. 요청 바디에 parentCommentId를 지정하면 해당 댓글의 답글로 생성됩니다 (1뎁스까지만 허용).")
  @PostMapping
  public ResponseEntity<ApiResponse<FeedCommentResponse.Created>> createComment(
      @PathVariable Long feedId,
      @CurrentUserId Long userId,
      @Valid @RequestBody FeedCommentRequest.CommentCreate request) {

    FeedCommentResponse.Created created = feedCommentService.createComment(feedId, request, userId);
    return ResponseEntity.ok(ApiResponse.success(created, "댓글이 생성되었습니다."));
  }

  @Operation(summary = "댓글 수정", description = "기존 댓글의 내용을 수정합니다.")
  @PutMapping("/{commentId}")
  public ResponseEntity<ApiResponse<FeedCommentResponse.Updated>> updateComment(
      @PathVariable Long feedId,
      @PathVariable Long commentId,
      @Valid @RequestBody FeedCommentRequest.CommentUpdate request) {

    FeedCommentResponse.Updated updated =
        feedCommentService.updateComment(feedId, commentId, request);

    return ResponseEntity.ok(ApiResponse.success(updated, "댓글이 수정되었습니다."));
  }

  @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<ApiResponse<Void>> deleteComment(
      @PathVariable Long feedId,
      @PathVariable Long commentId) {

    feedCommentService.deleteComment(feedId, commentId);
    return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다."));
  }

}
