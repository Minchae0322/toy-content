package com.example.toycontent.app.feed.controller;

import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import com.example.toycontent.app.feed.controller.dto.FeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.FeedCursorResponse;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.ListView;
import com.example.toycontent.app.feed.controller.dto.FeedSearchCondition;
import com.example.toycontent.app.feed.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FeedController", description = "피드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/feeds")
public class FeedController {
  private final FeedService feedService;

  @Operation(summary = "피드 목록 조회 (페이징)", description = "피드 목록을 페이징하여 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ListView>>> getFeeds(
      @ParameterObject Pageable pageable,
      @ParameterObject @ModelAttribute FeedSearchCondition condition) {

    Page<FeedResponse.ListView> feeds = feedService.getFeeds(pageable, condition);
    return ResponseEntity.ok(ApiResponse.success(feeds));
  }

  // 새로운 커서 기반 API (클라이언트용)
  @Operation(summary = "피드 목록 조회 (커서 페이징)", description = "인피니티 스크롤용 커서 기반 API")
  @GetMapping("/scroll")
  public ResponseEntity<ApiResponse<FeedCursorResponse>> getFeedsWithCursor(
      @ParameterObject @ModelAttribute FeedSearchCondition condition) {

    FeedCursorResponse feeds = feedService.getFeedsWithCursor(condition);
    return ResponseEntity.ok(ApiResponse.success(feeds));
  }

  @Operation(summary = "피드 전체 목록 조회", description = "피드 전체 목록을 조회합니다.")
  @GetMapping("/list")
  public ResponseEntity<ApiResponse<List<ListView>>> getFeedList(
      @ParameterObject @ModelAttribute FeedSearchCondition condition) {

    List<FeedResponse.ListView> feeds = feedService.getFeedList(condition);
    return ResponseEntity.ok(ApiResponse.success(feeds));
  }

  @Operation(summary = "피드 단건 조회", description = "특정 피드의 상세 정보를 조회합니다.")
  @GetMapping("/{feedId}")
  public ResponseEntity<ApiResponse<FeedResponse.Detail>> getFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId) {

    FeedResponse.Detail feed = feedService.getFeed(feedId);
    return ResponseEntity.ok(ApiResponse.success(feed));
  }

  @Operation(summary = "피드 생성", description = "새로운 피드를 생성합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<FeedResponse.Detail>> createFeed(
      @Valid @RequestBody FeedRequest.CreateFeed request) {

    FeedResponse.Detail feed = feedService.createFeed(request);
    return ResponseEntity.ok(ApiResponse.success(feed, "피드가 생성되었습니다."));
  }

  @Operation(summary = "피드 수정", description = "기존 피드 정보를 수정합니다.")
  @PutMapping("/{feedId}")
  public ResponseEntity<ApiResponse<FeedResponse.Detail>> updateFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId,
      @Valid @RequestBody FeedRequest.UpdateFeed request) {

    FeedResponse.Detail feed = feedService.updateFeed(feedId, request);
    return ResponseEntity.ok(ApiResponse.success(feed, "피드가 수정되었습니다."));
  }

  @Operation(summary = "피드 삭제", description = "피드를 삭제합니다.")
  @DeleteMapping("/{feedId}")
  public ResponseEntity<ApiResponse<Void>> deleteFeed(
      @Parameter(description = "피드 ID") @PathVariable Long feedId) {

    feedService.deleteFeed(feedId);
    return ResponseEntity.ok(ApiResponse.success(null, "피드가 삭제되었습니다."));
  }

}
