package com.example.toycontent.app.reward.badge.service;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserBadgeInfo;
import com.example.toycontent.app.reward.badge.domain.Badge;
import com.example.toycontent.app.reward.badge.domain.UserBadge;
import com.example.toycontent.app.reward.badge.repository.UserBadgeRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBadgeService {

  private final UserBadgeRepository userBadgeRepository;
  private final BadgeService badgeService;

  @Transactional
  public UserBadge awardBadge(Long userId, String badgeCode) {
    Badge badge = badgeService.getBadgeByCode(badgeCode);
    if (userBadgeRepository.existsByUserIdAndBadgeIdAndRevokedFalse(userId, badge.getId())) {
      throw new RestApiException(RewardErrorCode.BADGE_ALREADY_ACQUIRED);
    }
    return userBadgeRepository.save(createUserBadge(userId, badge));
  }

  @Transactional
  public UserBadge awardBadgeIfAbsent(Long userId, String badgeCode) {
    Badge badge = badgeService.getBadgeByCode(badgeCode);
    if (userBadgeRepository.existsByUserIdAndBadgeIdAndRevokedFalse(userId, badge.getId())) {
      return null;
    }
    return userBadgeRepository.save(createUserBadge(userId, badge));
  }

  @Transactional
  public void revokeBadge(Long userId, Long badgeId, String reason) {
    UserBadge userBadge = userBadgeRepository.findByUserIdAndBadgeId(userId, badgeId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.USER_BADGE_NOT_FOUND));
    if (userBadge.getRevoked()) {
      throw new RestApiException(RewardErrorCode.BADGE_ALREADY_REVOKED);
    }
    userBadge.revoke(reason);
  }

  public List<UserBadgeInfo> getUserBadges(Long userId) {
    return userBadgeRepository.findUserBadgesWithBadgeDetail(userId);
  }

  public long getUserBadgeCount(Long userId) {
    return userBadgeRepository.countByUserIdAndRevokedFalse(userId);
  }

  public boolean hasBadge(Long userId, String badgeCode) {
    Badge badge = badgeService.getBadgeByCode(badgeCode);
    return userBadgeRepository.existsByUserIdAndBadgeIdAndRevokedFalse(userId, badge.getId());
  }

  @Transactional
  public void pinBadge(Long userId, Long userBadgeId) {
    UserBadge userBadge = userBadgeRepository.findById(userBadgeId)
        .filter(ub -> ub.getUserId().equals(userId))
        .orElseThrow(() -> new RestApiException(RewardErrorCode.USER_BADGE_NOT_FOUND));
    userBadge.pin();
  }

  @Transactional
  public void unpinBadge(Long userId, Long userBadgeId) {
    UserBadge userBadge = userBadgeRepository.findById(userBadgeId)
        .filter(ub -> ub.getUserId().equals(userId))
        .orElseThrow(() -> new RestApiException(RewardErrorCode.USER_BADGE_NOT_FOUND));
    userBadge.unpin();
  }

  private UserBadge createUserBadge(Long userId, Badge badge) {
    return UserBadge.builder()
        .userId(userId)
        .badge(badge)
        .acquiredAt(LocalDateTime.now())
        .build();
  }
}
