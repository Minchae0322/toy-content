package com.example.toycontent.app.battle.controller.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public abstract class BattleItemCommentRequest {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "한줄 변론 작성 요청")
  public static class Create {

    @NotBlank(message = "변론 내용을 입력해주세요.")
    @Size(max = 40, message = "변론은 40자 이내로 작성해주세요.")
    @Schema(description = "변론 내용", example = "가성비로는 스벅 아아를 이길 수가 없음")
    private String content;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "한줄 변론 수정 요청")
  public static class Update {

    @NotBlank(message = "변론 내용을 입력해주세요.")
    @Size(max = 40, message = "변론은 40자 이내로 작성해주세요.")
    @Schema(description = "변론 내용", example = "역시 가성비 끝판왕")
    private String content;
  }
}