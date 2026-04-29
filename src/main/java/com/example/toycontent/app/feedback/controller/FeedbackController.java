package com.example.toycontent.app.feedback.controller;

import com.example.toycontent.app.common.annotation.CheckAdmin;
import com.example.toycontent.app.common.response.ApiResponse;
import com.example.toycontent.app.feedback.controller.dto.FeedbackRequest;
import com.example.toycontent.app.feedback.controller.dto.FeedbackResponse.ListView;
import com.example.toycontent.app.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FeedbackController", description = "의견 보내기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/feedbacks")
public class FeedbackController {

  private final FeedbackService feedbackService;

  @Operation(summary = "의견 등록", description = "비회원도 가능한 의견 등록 API")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> create(
      @Valid @RequestBody FeedbackRequest.Create request) {

    feedbackService.create(request);
    return ResponseEntity.ok(ApiResponse.success(null, "의견이 등록되었습니다."));
  }

  @Operation(summary = "의견 목록 조회 (관리자)", description = "관리자만 조회 가능. 페이징 지원")
  @CheckAdmin
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ListView>>> getFeedbacks(
      @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<ListView> feedbacks = feedbackService.getFeedbacks(pageable);
    return ResponseEntity.ok(ApiResponse.success(feedbacks));
  }
}
