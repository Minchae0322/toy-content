package com.example.toycontent.app.reward.badge.service;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardRequest;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.BadgeInfo;
import com.example.toycontent.app.reward.badge.domain.Badge;
import com.example.toycontent.app.reward.badge.repository.BadgeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

  private final BadgeRepository badgeRepository;

  @Transactional
  public BadgeInfo createBadge(RewardRequest.CreateBadge request) {
    if (badgeRepository.existsByCode(request.getCode())) {
      throw new RestApiException(RewardErrorCode.BADGE_CODE_DUPLICATED);
    }
    return BadgeInfo.from(badgeRepository.save(request.toEntity()));
  }

  @Transactional
  public BadgeInfo updateBadge(Long badgeId, RewardRequest.UpdateBadge request) {
    Badge badge = getBadgeById(badgeId);
    badge.update(request.getName(), request.getDescription(),
        request.getIconEmoji(), request.getIconImageUrl(), request.getCategory());
    return BadgeInfo.from(badge);
  }

  @Transactional
  public void deactivateBadge(Long badgeId) {
    Badge badge = getBadgeById(badgeId);
    badge.deactivate();
  }

  public Badge getBadgeById(Long badgeId) {
    return badgeRepository.findById(badgeId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.BADGE_NOT_FOUND));
  }

  public Badge getBadgeByCode(String code) {
    return badgeRepository.findByCode(code)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.BADGE_NOT_FOUND));
  }

  public List<BadgeInfo> getActiveBadges() {
    return badgeRepository.findByActivatedTrue().stream()
        .map(BadgeInfo::from)
        .toList();
  }

  public List<BadgeInfo> getBadgesByCategory(String category) {
    return badgeRepository.findByCategory(category).stream()
        .map(BadgeInfo::from)
        .toList();
  }
}
