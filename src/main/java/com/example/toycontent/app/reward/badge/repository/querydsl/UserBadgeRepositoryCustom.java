package com.example.toycontent.app.reward.badge.repository.querydsl;

import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserBadgeInfo;
import java.util.List;

public interface UserBadgeRepositoryCustom {

  List<UserBadgeInfo> findUserBadgesWithBadgeDetail(Long userId);
}
