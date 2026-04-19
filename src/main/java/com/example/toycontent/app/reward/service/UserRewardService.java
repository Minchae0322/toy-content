package com.example.toycontent.app.reward.service;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.domain.ExpHistory;
import com.example.toycontent.app.reward.domain.UserReward;
import com.example.toycontent.app.reward.repository.ExpHistoryRepository;
import com.example.toycontent.app.reward.repository.UserRewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRewardService {

  private final UserRewardRepository userRewardRepository;
  private final ExpHistoryRepository expHistoryRepository;

  @Transactional
  public UserReward getOrCreateUserReward(Long userId) {
    return userRewardRepository.findByUserId(userId)
            .orElseGet(() -> userRewardRepository.save(createUserReward(userId)));
  }

  @Transactional
  public UserReward addExp(Long userId, long amount, ExpSource source, Long sourceId) {
    if (amount <= 0) {
      throw new RestApiException(RewardErrorCode.INVALID_EXP_AMOUNT);
    }
    UserReward userReward = getOrCreateUserRewardWithLock(userId);
    userReward.addExp(amount);

    expHistoryRepository.save(createExpHistory(userId, amount, source, sourceId, userReward.getTotalExp()));

    return userReward;
  }

  public UserReward getUserReward(Long userId) {
    return userRewardRepository.findByUserId(userId)
        .orElseThrow(() -> new RestApiException(RewardErrorCode.USER_REWARD_NOT_FOUND));
  }

  private UserReward getOrCreateUserRewardWithLock(Long userId) {
    return userRewardRepository.findByUserIdWithLock(userId)
        .orElseGet(() -> userRewardRepository.save(createUserReward(userId)));
  }

  public Page<ExpHistory> getExpHistory(Long userId, Pageable pageable) {
    return expHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
  }

  @Transactional
  public void addSeasonExp(Long userId, long amount, String seasonCode) {
    UserReward userReward = userRewardRepository.findByUserIdWithLock(userId)
        .orElseGet(() -> userRewardRepository.save(createUserRewardWithSeason(userId, seasonCode)));
    userReward.addSeasonExp(amount, seasonCode);
  }

  private UserReward createUserReward(Long userId) {
    return UserReward.builder()
            .userId(userId)
            .build();
  }

  private UserReward createUserRewardWithSeason(Long userId, String seasonCode) {
    return UserReward.builder().userId(userId).seasonCode(seasonCode).build();
  }

  private ExpHistory createExpHistory(Long userId, long amount, ExpSource source, Long sourceId, long resultTotalExp) {
    return ExpHistory.builder()
        .userId(userId)
        .amount(amount)
        .source(source)
        .sourceId(sourceId)
        .resultTotalExp(resultTotalExp)
        .build();
  }
}
