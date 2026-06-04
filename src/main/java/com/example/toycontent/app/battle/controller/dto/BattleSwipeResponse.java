package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.common.enumuration.BattleItemType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class BattleSwipeResponse {

  @Schema(description = "스와이프 처리 결과")
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SwipeAck {

    @Schema(description = "처리된 아이템 ID", example = "10")
    private Long itemId;

    @Schema(description = "현재까지 진행한 아이템 수", example = "5")
    private Integer completedCount;

    @Schema(description = "전체 아이템 수", example = "20")
    private Integer totalCount;
  }

  @Schema(description = "스와이프 대상 다음 아이템 목록 (미진행만)")
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class NextItems {

    @Schema(description = "다음에 보여줄 아이템 목록 (랜덤 순)")
    private List<NextItem> items;

    @Schema(description = "현재까지 진행한 아이템 수", example = "5")
    private Integer completedCount;

    @Schema(description = "전체 아이템 수", example = "20")
    private Integer totalCount;
  }

  @Schema(description = "다음 스와이프 후보 아이템")
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class NextItem {

    @Schema(description = "아이템 ID")
    private Long id;

    @Schema(description = "아이템 타입")
    private BattleItemType itemType;

    @Schema(description = "표시명 (CUSTOM/YOUTUBE는 customName, PRODUCT는 product.name)")
    private String displayName;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "유튜브 임베드 URL (YOUTUBE 한정)")
    private String embedUrl;

    public static NextItem from(BattleItem item) {
      return NextItem.builder()
          .id(item.getId())
          .itemType(item.getItemType())
          .displayName(item.getDisplayName())
          .imageUrl(item.getDisplayImageUrl())
          .embedUrl(item.getEmbedUrl())
          .build();
    }
  }

  @Schema(description = "스와이프 배틀 결과 (랭킹)")
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Result {

    @Schema(description = "랭킹 정렬된 아이템 목록")
    private List<ResultItem> items;
  }

  @Schema(description = "결과 1건 — 단순 합산 (strong*3 + pick*1) 내림차순")
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ResultItem {

    @Schema(description = "랭킹 (1부터)", example = "1")
    private Integer rank;

    @Schema(description = "아이템 ID")
    private Long id;

    @Schema(description = "표시명")
    private String displayName;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "강추 PICK 수")
    private Integer strongPickCount;

    @Schema(description = "PICK 수")
    private Integer pickCount;

    @Schema(description = "PASS 수")
    private Integer passCount;

    @Schema(description = "랭킹 점수 (strong*3 + pick*1)")
    private Integer score;
  }
}
