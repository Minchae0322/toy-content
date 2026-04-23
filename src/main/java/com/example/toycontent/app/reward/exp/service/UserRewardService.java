package com.example.toycontent.app.reward.exp.service;

import com.example.toycontent.app.common.enumuration.ExpSource;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.RewardErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserRewardInfo;
import com.example.toycontent.app.reward.exp.service.dto.LevelInfo;
import com.example.toycontent.app.reward.exp.domain.ExpHistory;
import com.example.toycontent.app.reward.exp.domain.LevelExp;
import com.example.toycontent.app.reward.exp.domain.UserReward;
import com.example.toycontent.app.reward.exp.repository.ExpHistoryRepository;
import com.example.toycontent.app.reward.exp.repository.UserRewardRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final LevelExpService levelExpService;

  @Transactional
  public UserRewardInfo getOrCreateUserRewardInfo(Long userId) {
    UserReward reward = userRewardRepository.findByUserId(userId)
        .orElseGet(() -> userRewardRepository.save(createUserReward(userId)));

    return UserRewardInfo.of(reward, levelExpService.computeLevelInfo(reward.getTotalExp()));
  }

  public UserRewardInfo getUserRewardInfo(Long userId) {
    UserReward reward = userRewardRepository.findByUserId(userId)
        .orElseGet(() -> createUserReward(userId));

    return UserRewardInfo.of(reward, levelExpService.computeLevelInfo(reward.getTotalExp()));
  }

  public Map<Long, UserRewardInfo> getUserRewardInfoMap(Collection<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<Long, UserReward> existing = userRewardRepository.findAllByUserIdIn(userIds)
        .stream()
        .collect(Collectors.toMap(UserReward::getUserId, Function.identity()));

    List<LevelExp> levelTable = levelExpService.getLevelTable();

    return userIds.stream()
        .distinct()
        .collect(Collectors.toMap(
            Function.identity(),
            userId -> {
              UserReward reward = existing.getOrDefault(userId, createUserReward(userId));

              return UserRewardInfo.of(reward, levelExpService.computeLevelInfo(reward.getTotalExp(), levelTable));
            }));
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
