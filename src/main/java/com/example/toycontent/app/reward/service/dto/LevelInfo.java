package com.example.toycontent.app.reward.service.dto;

import com.example.toycontent.app.common.enumuration.UserTier;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "레벨 계산 결과")
public record LevelInfo(

    @Schema(description = "현재 레벨 (1~40)", example = "12")
    int level,

    @Schema(description = "현재 티어 (PLAIN / FRUITY / GRANOLA / PARFAIT / SIGNATURE)")
    UserTier tier,

    @Schema(description = "현재 레벨에서 쌓은 EXP", example = "350")
    long currentLevelExp,

    @Schema(description = "다음 레벨까지 남은 EXP (최대 레벨이면 0)", example = "650")
    long nextLevelExp,

    @Schema(description = "최대 레벨(40) 도달 여부", example = "false")
    boolean maxLevel
) {
}
