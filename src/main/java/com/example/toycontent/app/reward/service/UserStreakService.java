package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.domain.UserStreak;
import com.example.toycontent.app.reward.repository.UserStreakRepository;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStreakService {

  private static final Set<Integer> STREAK_MILESTONES = Set.of(3, 7, 14, 30, 100);

  private final UserStreakRepository userStreakRepository;
  private final ExpGrantService expGrantService;

  @Transactional
  public UserStreak getOrCreateUserStreak(Long userId) {
    return userStreakRepository.findByUserId(userId)
            .orElseGet(() -> userStreakRepository.save(createStreak(userId)));
  }

  private UserStreak createStreak(Long userId) {
    return UserStreak.builder().userId(userId).build();
  }

  @Transactional
  public UserStreak recordPosting(Long userId) {
    UserStreak streak = getOrCreateUserStreak(userId);
    boolean recorded = streak.recordPosting(LocalDate.now());
    if (!recorded) {
      log.debug("이미 오늘 인증 완료 - userId: {}", userId);
      return streak;
    }

    // 스트릭 마일스톤 보너스 EXP 지급
    int current = streak.getCurrentStreak();
    if (STREAK_MILESTONES.contains(current)) {
      expGrantService.grantStreakBonus(userId, current);
      log.info("스트릭 마일스톤 달성 - userId: {}, streak: {}", userId, current);
    }

    return streak;
  }

  public UserStreak getUserStreak(Long userId) {
    return userStreakRepository.findByUserId(userId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.USER_STREAK_NOT_FOUND));
  }



  public int getCurrentStreak(Long userId) {
    return userStreakRepository.findByUserId(userId)
        .map(UserStreak::getCurrentStreak)
        .orElse(0);
  }

  @Transactional
  public UserStreak useRecoveryTicket(Long userId) {
    UserStreak streak = getUserStreak(userId);
    if (streak.getRecoveryTickets() <= 0) {
      throw new RestApiException(RewardErrorCode.NO_RECOVERY_TICKET);
    }
    streak.useRecoveryTicket();
    return streak;
  }

  @Transactional
  public void grantRecoveryTicket(Long userId, int count) {
    UserStreak streak = getOrCreateUserStreak(userId);
    streak.grantRecoveryTickets(count);
  }
}
