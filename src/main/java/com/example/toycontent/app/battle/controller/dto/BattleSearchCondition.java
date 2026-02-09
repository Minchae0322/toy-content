package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.common.enumuration.BattleStatus;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleSearchCondition {

  @Parameter(description = "카테고리")
  private Long category;

  @Parameter(description = "카테고리 뎁스")
  private Integer categoryDepth;

  @Parameter(description = "배틀 상태 (NORMAL, SUSPENDED, EARLY_CLOSED)")
  private BattleStatus status;

  @Parameter(description = "검색어 (제목)")
  private String keyword;

  @Parameter(description = "생성자 ID")
  private Long creatorId;

  @Parameter(description = "진행 여부")
  private Boolean isActive;

}
