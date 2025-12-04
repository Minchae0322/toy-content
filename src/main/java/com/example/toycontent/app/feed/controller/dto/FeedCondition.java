package com.example.toycontent.app.feed.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public abstract class FeedCondition {


  /**
   * 탐색/검색 피드 조회 조건
   * GET /api/feeds/scroll
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Search {

    @Parameter(description = "검색 키워드")
    private String keyword;

    @Parameter(description = "활성화 상태")
    private Boolean isActive;

    @Parameter(description = "카테고리 ID")
    private Long categoryId;

    @Parameter(description = "해시태그 목록")
    private List<String> hashtags;

    @Parameter(description = "피드 작성자 ID")
    private Long creatorId;

    @Parameter(description = "커서 (마지막 조회한 피드 ID)")
    private Long cursor;

    @Parameter(description = "조회 개수")
    private Integer size;

    @JsonIgnore
    @Schema(hidden = true)
    private Long readerId;
  }

  /**
   * 홈 피드 조회 조건 (팔로우 기반)
   * GET /api/feeds/home
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Following {

    @Parameter(description = "커서 (마지막 조회한 피드 ID)")
    private Long cursor;

    @Parameter(description = "조회 개수")
    private Integer size = 20;

    @JsonIgnore
    @Schema(hidden = true)
    private Long readerId;
  }




}
