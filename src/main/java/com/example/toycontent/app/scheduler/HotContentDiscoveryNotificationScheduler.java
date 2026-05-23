package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.notification.hotcontent.HotContentCandidate;
import com.example.toycontent.app.notification.hotcontent.HotContentNotificationSent;
import com.example.toycontent.app.notification.hotcontent.HotContentNotificationSentRepository;
import com.example.toycontent.app.notification.hotcontent.HotContentSource;
import com.example.toycontent.app.notification.hotcontent.HotContentType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매일 새벽 4시, 등록된 {@link HotContentSource}들로부터 후보를 모아 hotScore가 가장 높고
 * 아직 발송된 적 없는 콘텐츠 1건을 골라 푸시 동의자 전원에게 브로드캐스트한다.
 *
 * <p>발송 대상 콘텐츠 종류 확장 = {@link HotContentSource} 구현체 추가 + {@link HotContentType}
 * 항목 1줄. 스케줄러 본문은 수정 불필요.
 *
 * <p>{@code (content_type, content_id)} 단위 영구 dedup —
 * 한 번 발송된 콘텐츠는 재후보에서 영구 제외된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HotContentDiscoveryNotificationScheduler {

  /** Source별로 가져올 후보 수. 단일 콘텐츠 종류 내 미발송 풀을 확보하기 위한 여유 N. */
  private static final int CANDIDATE_LIMIT_PER_SOURCE = 20;

  private final List<HotContentSource> hotContentSources;
  private final HotContentNotificationSentRepository sentRepository;
  private final NotificationService notificationService;

  @Scheduled(cron = "0 0 4 * * *")
  @SchedulerLock(name = "hotContentDiscoveryDaily", lockAtLeastFor = "5m", lockAtMostFor = "30m")
  @Transactional
  public void notifyDailyHotContent() {
    List<HotContentCandidate> allCandidates = collectAllCandidates();
    if (allCandidates.isEmpty()) {
      log.info("[핫 콘텐츠 알림] 후보 없음 — 스킵");
      return;
    }

    Optional<HotContentCandidate> picked = pickTopUnsent(allCandidates);
    if (picked.isEmpty()) {
      log.info("[핫 콘텐츠 알림] 후보 {}건 전부 기 발송 — 스킵", allCandidates.size());
      return;
    }

    HotContentCandidate winner = picked.get();
    notificationService.broadcastHotContentDiscovery(winner);
    sentRepository.save(HotContentNotificationSent.of(winner.getType(), winner.getContentId()));

    log.info("[핫 콘텐츠 알림] 브로드캐스트 완료 - type={}, contentId={}, hotScore={}",
        winner.getType(), winner.getContentId(), winner.getHotScore());
  }

  private List<HotContentCandidate> collectAllCandidates() {
    List<HotContentCandidate> all = new ArrayList<>();
    for (HotContentSource source : hotContentSources) {
      all.addAll(source.findTopCandidates(CANDIDATE_LIMIT_PER_SOURCE));
    }
    return all;
  }

  /**
   * 후보를 hotScore desc로 정렬해 발송 이력이 없는 첫 번째 후보를 반환.
   * 이력 조회는 콘텐츠 종류별 IN 쿼리 1회씩으로 끝낸다.
   */
  private Optional<HotContentCandidate> pickTopUnsent(List<HotContentCandidate> candidates) {
    Map<HotContentType, Set<Long>> sentIdsByType = loadSentIdsByType(candidates);

    return candidates.stream()
        .sorted(Comparator.comparingDouble(HotContentCandidate::getHotScore).reversed())
        .filter(c -> !sentIdsByType.getOrDefault(c.getType(), Set.of()).contains(c.getContentId()))
        .findFirst();
  }

  private Map<HotContentType, Set<Long>> loadSentIdsByType(List<HotContentCandidate> candidates) {
    Map<HotContentType, List<Long>> idsByType = candidates.stream()
        .collect(Collectors.groupingBy(
            HotContentCandidate::getType,
            Collectors.mapping(HotContentCandidate::getContentId, Collectors.toList())
        ));

    Map<HotContentType, Set<Long>> result = new HashMap<>();
    idsByType.forEach((type, ids) -> {
      Set<Long> sentIds = sentRepository.findByContentTypeAndContentIdIn(type, ids).stream()
          .map(HotContentNotificationSent::getContentId)
          .collect(Collectors.toSet());
      result.put(type, sentIds);
    });
    return result;
  }
}
