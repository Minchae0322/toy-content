package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.enumuration.UserTier;

public record LevelInfo(
    int level,
    UserTier tier,
    long currentLevelExp,
    long nextLevelExp,
    boolean maxLevel
) {
}
