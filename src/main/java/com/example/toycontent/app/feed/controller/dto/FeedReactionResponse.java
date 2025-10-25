package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FeedReactionResponse {

  @Getter
  @Builder
  public static class ReactionResult {

    private FeedReactionType reactionType;
    private String action; // "added", "removed", "changed"
    private String message;

    public static ReactionResult added(FeedReactionType reactionType) {
      return ReactionResult.builder()
          .reactionType(reactionType)
          .action("added")
          .message("리액션이 추가되었습니다.")
          .build();
    }

    public static ReactionResult removed(FeedReactionType reactionType) {
      return ReactionResult.builder()
          .reactionType(reactionType)
          .action("removed")
          .message("리액션이 취소되었습니다.")
          .build();
    }

    public static ReactionResult changed(FeedReactionType reactionType) {
      return ReactionResult.builder()
          .reactionType(reactionType)
          .action("changed")
          .message("리액션이 변경되었습니다.")
          .build();
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "반응 통계")
  public static class ReactionStats {

    @Schema(description = "전체 반응 수")
    private Long totalCount;

    @Schema(description = "반응 타입별 개수")
    private Map<FeedReaction, Long> countByType = new HashMap<>();

    @Schema(description = "좋아요 수")
    private Long likeCount;

    @Schema(description = "최고예요 수")
    private Long hotCount;


    public static ReactionStats from(List<FeedReaction> reactions) {
      if (reactions == null || reactions.isEmpty()) {
        return ReactionStats.builder()
            .totalCount(0L)
            .countByType(Map.of())
            .likeCount(0L)
            .build();
      }

      Map<FeedReactionType, Long> countByType = reactions.stream()
          .collect(Collectors.groupingBy(
              FeedReaction::getReactionType,
              Collectors.counting()
          ));

      return ReactionStats.builder()
          .totalCount((long) reactions.size())
          .likeCount(countByType.getOrDefault(FeedReactionType.LIKE, 0L))
          .hotCount(countByType.getOrDefault(FeedReactionType.HOT, 0L))
          .build();
    }
  }

  @Getter
  @Builder
  public static class UserReaction {

    private FeedReactionType reactionType;
    private boolean hasReaction;

    public static UserReaction from(FeedReaction reaction) {
      if (reaction == null) {
        return UserReaction.builder()
            .hasReaction(false)
            .build();
      }

      return UserReaction.builder()
          .reactionType(reaction.getReactionType())
          .hasReaction(true)
          .build();
    }
  }
}
