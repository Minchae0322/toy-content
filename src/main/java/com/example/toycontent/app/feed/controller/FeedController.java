package com.example.toycontent.app.feed.controller;

import com.example.toycontent.app.common.annotation.CurrentUserId;
import com.example.toycontent.app.common.annotation.CurrentUserIsAdmin;
import com.example.toycontent.app.common.dto.CursorResponse;
import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.controller.dto.FeedReactionResponse;
import com.example.toycontent.app.feed.controller.dto.FeedReportRequest;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import com.example.toycontent.app.feed.controller.dto.FeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.FeedCursorResponse;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.HotFeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.ListView;
import com.example.toycontent.app.feed.service.FeedReactionService;
import com.example.toycontent.app.feed.service.FeedReportService;
import com.example.toycontent.app.feed.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FeedController", description = "피드 API")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(value = "/feeds")
public class FeedController {
  private final FeedService feedService;
  private final FeedReactionService feedReactionService;
  private final FeedReportService feedReportService;

  @Operation(summary = "피드 목록 조회 (커서 페이징)", description = "인피니티 스크롤용 커서 기반 API")
  @GetMapping("/scroll")
  public ResponseEntity<ApiResponse<CursorResponse<ListView>>> getFeedsWithCursor(
      @ParameterObject @ModelAttribute Search condition,
      @CurrentUserId(required = false) Long userId) {

    CursorResponse<ListView> feeds = feedService.getFeedsWithCursor(condition, userId);
    return ResponseEntity.ok(ApiResponse.success(feeds));
  }

  @Operation(summary = "팔로잉 피드 조회 (커서 페이징)", description = "팔로우한 사용자의 피드를 조회")
  @GetMapping("/following")
  public ResponseEntity<ApiResponse<FeedCursorResponse>> getFollowingFeeds(
      @ParameterObject @ModelAttribute FeedCondition.Following condition,
      @CurrentUserId Long userId) {

    FeedCursorResponse feeds = feedService.getFollowingFeeds(condition, userId);
    return ResponseEntity.ok(ApiResponse.success(feeds));
  }

  @Operation(
      summary = "핫 피드 목록 조회",
      description = """
                    인기도 점수가 높은 피드를 조회합니다.
          
                    **핫 스코어 계산 공식:**
          ```
                    hotScore = (좋아요 * 2 + 핫 * 3 + 조회수 * 0.1) / 시간 감쇠 계수
                    시간 감쇠 계수 = (경과 시간(시) + 12)^1.2
          ```
          
                    **특징:**
                    - 최근 게시물일수록 높은 점수
                    - 좋아요보다 핫 리액션에 더 높은 가중치
                    - 시간이 지날수록 점수 자동 하락
                    - Reddit/Hacker News 알고리즘 적용
          """
  )
  @GetMapping("/hot")
  public ResponseEntity<ApiResponse<Page<HotFeedResponse>>> getHotFeeds(
      @ParameterObject @PageableDefault(sort = "hotScore", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<FeedResponse.HotFeedResponse> feeds = feedService.getHotFeeds(pageable);
    return ResponseEntity.ok(ApiResponse.success(feeds));
  }

  @Operation(summary = "피드 단건 조회", description = "특정 피드의 상세 정보를 조회합니다.")
  @GetMapping("/{feedId}")
  public ResponseEntity<ApiResponse<FeedResponse.Detail>> getFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @CurrentUserId(required = false) Long userId) {

    // 조회수 증가는 getFeed가 발행하는 FeedViewedEvent를 리스너가 커밋 후 처리한다 (2026-08-23)
    FeedResponse.Detail feed = feedService.getFeed(feedId, userId);
    return ResponseEntity.ok(ApiResponse.success(feed));
  }

  @Operation(summary = "피드 생성", description = "새로운 피드를 생성합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<FeedResponse.FeedCreated>> createFeed(
      @Valid @RequestBody FeedRequest.CreateFeed request) {

    FeedResponse.FeedCreated feed = feedService.createFeed(request);
    return ResponseEntity.ok(ApiResponse.success(feed, "피드가 생성되었습니다."));
  }

  @Operation(summary = "피드 수정", description = "기존 피드 정보를 수정합니다.")
  @PutMapping("/{feedId}")
  public ResponseEntity<ApiResponse<FeedResponse.FeedCreated>> updateFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @Valid @RequestBody FeedRequest.UpdateFeed request,
      @CurrentUserId Long userId) {

    FeedResponse.FeedCreated feed = feedService.updateFeed(feedId, request, userId);
    return ResponseEntity.ok(ApiResponse.success(feed, "피드가 수정되었습니다."));
  }

  @Operation(summary = "피드 삭제", description = "피드를 삭제합니다.")
  @DeleteMapping("/{feedId}")
  public ResponseEntity<ApiResponse<Void>> deleteFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @CurrentUserId Long userId,
      @CurrentUserIsAdmin boolean isAdmin) {

    feedService.deleteFeed(feedId, userId, isAdmin);
    return ResponseEntity.ok(ApiResponse.success(null, "피드가 삭제되었습니다."));
  }

  @Operation(summary = "피드 리액션 토글", description = "피드에 리액션을 추가/제거/변경합니다.")
  @PostMapping("/{feedId}/reactions")
  public ResponseEntity<ApiResponse<FeedReactionResponse.ReactionResult>> toggleReaction(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @Parameter(description = "리액션 타입") @RequestParam FeedReactionType reactionType,
      @CurrentUserId Long userId) {

    FeedReactionResponse.ReactionResult result =
        feedReactionService.toggleReaction(feedId, userId, reactionType);
    return ResponseEntity.ok(ApiResponse.success(result));
  }

  @Operation(summary = "피드 리액션 제거", description = "피드의 리액션을 제거합니다.")
  @DeleteMapping("/{feedId}/reactions")
  public ResponseEntity<ApiResponse<Void>> removeReaction(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @Parameter(description = "리액션 타입") @RequestParam FeedReactionType reactionType,
      @CurrentUserId Long userId) {

    feedReactionService.removeReaction(feedId, userId, reactionType);
    return ResponseEntity.ok(ApiResponse.success(null, "리액션이 제거되었습니다."));
  }

  @Operation(summary = "피드 신고", description = "부적절한 피드를 신고합니다. 동일 피드는 한 번만 신고할 수 있습니다.")
  @PostMapping("/{feedId}/reports")
  public ResponseEntity<ApiResponse<Long>> reportFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @Valid @RequestBody FeedReportRequest request,
      @CurrentUserId Long userId) {

    Long reportId = feedReportService.report(feedId, userId, request);
    return ResponseEntity.ok(ApiResponse.success(reportId, "신고가 접수되었습니다."));
  }

}
