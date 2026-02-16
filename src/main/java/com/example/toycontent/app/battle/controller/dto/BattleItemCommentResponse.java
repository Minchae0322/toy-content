package com.example.toycontent.app.battle.controller.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class BattleItemCommentResponse {

  @Getter
  @Builder
  @Schema(description = "코멘트 상세")
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Detail {

    @Schema(description = "코멘트 ID")
    private Long commentId;

    @Schema(description = "배틀 아이템 ID")
    private Long battleItemId;

    @Schema(description = "작성자 ID")
    private Long creatorId;

    @Schema(description = "작성자 닉네임 (작성 시점)")
    private String creatorNickname;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String creatorProfileImageUrl;

    @Schema(description = "코멘트 내용")
    private String content;

    @Schema(description = "공감 수")
    private Integer likeCount;

    @Schema(description = "내가 공감했는지 여부")
    private Boolean isLiked;

    @Schema(description = "본인 작성 여부")
    private Boolean isMine;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;
  }

  @Getter
  @Builder
  @Schema(description = "공감 토글 결과")
  public static class LikeResult {

    @Schema(description = "공감 여부 (true: 공감함, false: 공감 취소)")
    private Boolean isLiked;

    @Schema(description = "현재 공감 수")
    private Integer likeCount;

    public static LikeResult of(boolean isLiked, int likeCount) {
      return LikeResult.builder()
          .isLiked(isLiked)
          .likeCount(likeCount)
          .build();
    }
  }

  @Getter
  @Builder
  @Schema(description = "아이템별 BEST 코멘트 + 코멘트 수")
  public static class BattleItemCommentSummary {

    @Schema(description = "배틀 아이템 ID")
    private Long battleItemId;

    @Schema(description = "BEST 코멘트 ID")
    private Long commentId;

    @Schema(description = "작성자 닉네임")
    private String creatorNickname;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String creatorProfileImageUrl;

    @Schema(description = "코멘트 내용")
    private String content;

    @Schema(description = "공감 수")
    private Integer likeCount;

    @Schema(description = "전체 코멘트 수")
    private Long commentCount;

    public static BattleItemCommentSummary from(Object[] row) {
      return BattleItemCommentSummary.builder()
          .battleItemId(((Number) row[0]).longValue())
          .commentId(((Number) row[1]).longValue())
          .creatorNickname((String) row[2])
          .creatorProfileImageUrl((String) row[3])
          .content((String) row[4])
          .likeCount(((Number) row[5]).intValue())
          .commentCount(((Number) row[6]).longValue())
          .build();
    }
  }



}