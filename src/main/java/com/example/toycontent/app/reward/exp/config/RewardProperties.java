package com.example.toycontent.app.reward.exp.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reward")
public record RewardProperties(long dailyExpCap, ZoneId timeZone) {
}
