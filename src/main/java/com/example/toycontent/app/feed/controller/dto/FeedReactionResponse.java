package com.example.toycontent.app.feed.controller.dto;

import com.example.toycontent.app.common.enumuration.FeedReactionType;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedReaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "피드 리액션 응답")
public class FeedReactionResponse {

  @Getter
  @Builder
  @Schema(description = "리액션 처리 결과")
  public static class ReactionResult {

    @Schema(description = "리액션 타입", example = "LIKE")
    private FeedReactionType reactionType;

    @Schema(description = "수행된 액션", example = "added", allowableValues = {"added", "removed", "changed"})
    private String action;

    @Schema(description = "결과 메시지", example = "리액션이 추가되었습니다.")
    private String message;

    @Schema(description = "좋아요 수", example = "42")
    private Integer likeCount;


    public static ReactionResult added(FeedReactionType reactionType, int likeCount) {
      return ReactionResult.builder()
          .reactionType(reactionType)
          .action("added")
          .message("리액션이 추가되었습니다.")
          .likeCount(likeCount)
          .build();
    }

    public static ReactionResult removed(FeedReactionType reactionType, int likeCount) {
      return ReactionResult.builder()
          .reactionType(reactionType)
          .action("removed")
          .message("리액션이 취소되었습니다.")
          .likeCount(likeCount)
          .build();
    }

  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "피드 리액션 통계")
  public static class ReactionStats {

    @Schema(description = "전체 리액션 수", example = "57")
    private Long totalCount;

    @Schema(description = "리액션 타입별 개수 (현재는 사용되지 않음)")
    private Map<FeedReaction, Long> countByType = new HashMap<>();

    @Schema(description = "좋아요 수", example = "42")
    private Long likeCount;



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
          .build();
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "사용자의 리액션 상태 (좋아요, 최고예요 여부)")
  public static class UserReactions {

    @Schema(description = "좋아요 눌렀는지 여부", example = "true")
    private boolean hasLike;

    @Schema(description = "핫해요 눌렀는지 여부", example = "false")
    private boolean hasHot;

    public static UserReactions from(List<FeedReaction> reactions) {
      if (reactions == null || reactions.isEmpty()) {
        return noReaction();
      }

      EnumSet<FeedReactionType> reactionTypes = reactions.stream()
          .map(FeedReaction::getReactionType)
          .collect(Collectors.toCollection(() -> EnumSet.noneOf(FeedReactionType.class)));

      return UserReactions.builder()
          .hasLike(reactionTypes.contains(FeedReactionType.LIKE))
          .hasHot(reactionTypes.contains(FeedReactionType.HOT))
          .build();
    }

    public static UserReactions noReaction() {
      return UserReactions.builder()
          .hasLike(false)
          .hasHot(false)
          .build();
    }
  }
}