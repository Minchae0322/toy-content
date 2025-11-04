package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.domain.BattleVote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "배틀 투표 응답")
public abstract class BattleVoteResponse {

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "사용자 배틀 투표 정보")
  public static class UserBattleVote {

    @Schema(description = "투표 ID", example = "1")
    private Long voteId;

    @Schema(description = "배틀 아이템 ID", example = "10")
    private Long itemId;

    @Schema(description = "투표한 사용자 ID", example = "100")
    private Long userId;

    @Schema(description = "투표 순위 (1위, 2위, 3위)", example = "1")
    private Integer rank;

    @Schema(description = "투표 점수", example = "3")
    private Integer score;

    public static UserBattleVote from(BattleVote battleVote) {
      return UserBattleVote.builder()
          .voteId(battleVote.getId())
          .itemId(battleVote.getBattleItem().getId())
          .userId(battleVote.getUserId())
          .rank(battleVote.getVoteRank())
          .score(battleVote.getScore())
          .build();
    }
  }
}
