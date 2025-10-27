package com.example.toycontent.app.feed.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedSearchCondition {

  @Parameter(description = "검색 키워드")
  private String keyword;

  @Parameter(description = "활성화 상태")
  private Boolean isActive;

  @Parameter(description = "카테고리 ID")
  private Long categoryId;

  @Parameter(description = "해시태그 목록")
  private List<String> hashtags;

  @Parameter(description = "커서 (마지막 조회한 피드 ID)")
  private Long cursor;

  @Schema(description = "Size")
  private Integer size;

  @JsonIgnore
  @Schema(description = "조회자 아이디", hidden = true)
  private Long readerId;

}
