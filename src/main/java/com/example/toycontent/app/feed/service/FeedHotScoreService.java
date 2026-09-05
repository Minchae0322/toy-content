package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.common.hotscore.HotScoreSettings;
import com.example.toycontent.app.config.CacheConfig;
import com.example.toycontent.app.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

/**
 * 피드 핫 스코어 전체 재계산 (수동 실행 전용).
 *
 * <p>평상시 점수는 좋아요·댓글·조회가 바뀌는 그 행에서 바로 갱신되므로 배치가 필요 없다.
 * 이 메서드는 {@code hot-score.feed-time-divisor-seconds}를 바꾼 뒤 저장된 점수를
 * 새 기준으로 맞출 때만 관리자 API에서 부른다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedHotScoreService {

  private final FeedRepository feedRepository;

  @Transactional
  @CacheEvict(cacheNames = CacheConfig.HOT_FEEDS, allEntries = true)
  public int recalculateAll() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    int count = feedRepository.recalculateAllHotScores(HotScoreSettings.feedDivisor());
    stopWatch.stop();
    log.info("[hot-score] 피드 전체 재계산 완료 - {}건, divisor={}s, {}ms",
        count, HotScoreSettings.feedDivisor(), stopWatch.getTotalTimeMillis());
    return count;
  }
}
