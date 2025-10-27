package com.example.toycontent.app.hashtag.controller;


import com.example.toycontent.app.hashtag.controller.dto.HashtagResponse.HotHashtagResponse;
import com.example.toycontent.app.hashtag.controller.dto.HashtagSearchCondition;
import com.example.toycontent.app.hashtag.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HashtagController", description = "해시태그 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/hashtags")
public class HashtagController {

  private final HashtagService hashtagService;

  @Operation(summary = "인기 해시태그 조회", description = "사용 횟수가 많은 순으로 해시태그를 조회합니다")
  @GetMapping("/hot")
  public ResponseEntity<Page<HotHashtagResponse>> getHotHashtags(
      @ModelAttribute HashtagSearchCondition condition,
      @ParameterObject @PageableDefault(size = 10, sort = "usageCount", direction = Sort.Direction.DESC) Pageable pageable

  ) {

    Page<HotHashtagResponse> hotHashtags = hashtagService.getHotHashtags(condition, pageable);
    return ResponseEntity.ok(hotHashtags);
  }
}
