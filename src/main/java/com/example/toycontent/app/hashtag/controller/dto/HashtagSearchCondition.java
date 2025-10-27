package com.example.toycontent.app.hashtag.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "해시태그 검색 조건")
public class HashtagSearchCondition {

  @Schema(description = "해시태그 이름 검색 (부분 일치)", example = "맛있")
  private String name;

  @Schema(description = "최소 사용 횟수", example = "10")
  private Long minUsageCount;
}
