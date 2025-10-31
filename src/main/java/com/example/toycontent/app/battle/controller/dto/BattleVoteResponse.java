package com.example.toycontent.app.battle.controller.dto;

import com.example.toycontent.app.battle.domain.BattleVote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class BattleVoteResponse {


  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class UserBattleVote {

    private Long voteId;
    private Long itemId;
    private Long userId;

    private Integer rank;
    private Integer score;

    public static UserBattleVote from(BattleVote battleVote) {
      return UserBattleVote.builder()
          .voteId(battleVote.getId())
          .itemId(battleVote.getBattleItem().getId())
          .userId(battleVote.getUserId())
          .rank(battleVote.getRank())
          .score(battleVote.getScore())
          .build();

    }

  }
}
