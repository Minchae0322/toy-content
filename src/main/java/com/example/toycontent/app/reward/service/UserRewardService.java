package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.domain.UserReward;
import com.example.toycontent.app.reward.repository.UserRewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRewardService {

  private final UserRewardRepository userRewardRepository;

  @Transactional
  public UserReward addExp(Long userId, long amount) {
    if (amount <= 0) {
      throw new RestApiException(RewardErrorCode.INVALID_EXP_AMOUNT);
    }
    UserReward userReward = getOrCreateUserRewardWithLock(userId);
    userReward.addExp(amount);
    return userReward;
  }

  public UserReward getUserReward(Long userId) {
    return userRewardRepository.findByUserId(userId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.USER_REWARD_NOT_FOUND));
  }

  private UserReward getOrCreateUserRewardWithLock(Long userId) {
    return userRewardRepository.findByUserIdWithLock(userId)
        .orElseGet(() -> userRewardRepository.save(
            UserReward.builder().userId(userId).build()));
  }

  public UserReward getOrCreateUserReward(Long userId) {
    return userRewardRepository.findByUserId(userId)
        .orElseGet(() -> userRewardRepository.save(
            UserReward.builder()
                .userId(userId)
                .build()));
  }

  @Transactional
  public void addSeasonExp(Long userId, long amount, String seasonCode) {
    UserReward userReward = userRewardRepository.findByUserIdWithLock(userId)
        .orElseGet(() -> userRewardRepository.save(
            UserReward.builder().userId(userId).seasonCode(seasonCode).build()));
    userReward.addSeasonExp(amount, seasonCode);
  }
}
