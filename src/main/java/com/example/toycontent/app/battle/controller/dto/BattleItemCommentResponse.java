package com.example.toycontent.app.battle.controller.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

public abstract class BattleItemCommentResponse {

  @Getter
  @Builder
  @Schema(description = "코멘트 상세")
  public static class Detail {

    @Schema(description = "코멘트 ID")
    private Long commentId;

    @Schema(description = "배틀 아이템 ID")
    private Long battleItemId;

    @Schema(description = "작성자 ID")
    private Long memberId;

    @Schema(description = "작성자 닉네임")
    private String nickname;

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
  }
}