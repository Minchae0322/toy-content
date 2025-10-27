package com.example.toycontent.app.hashtag.controller.dto;

import com.example.toycontent.app.hashtag.domain.Hashtag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public abstract class HashtagResponse {

  @Data
  @Builder
  @AllArgsConstructor
  @Schema(description = "인기 해시태그 응답")
  public static class HotHashtagResponse {

    @Schema(description = "해시태그 ID", example = "1")
    private Long id;

    @Schema(description = "해시태그 이름", example = "맛있어요")
    private String name;

    @Schema(description = "사용 횟수", example = "150")
    private Long usageCount;

    public static HotHashtagResponse from(Hashtag hashtag) {
      return HotHashtagResponse.builder()
          .id(hashtag.getId())
          .name(hashtag.getName())
          .usageCount(hashtag.getUsageCount())
          .build();
    }
  }
}
