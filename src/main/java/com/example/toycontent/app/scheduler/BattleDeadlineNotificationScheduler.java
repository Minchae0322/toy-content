package com.example.toycontent.app.scheduler;

import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.notification.BattleNotificationPhase;
import com.example.toycontent.app.notification.BattleNotificationSent;
import com.example.toycontent.app.notification.BattleNotificationSentRepository;
import com.example.toycontent.app.notification.NotificationService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleDeadlineNotificationScheduler {

  private final BattleRepository battleRepository;
  private final BattleVoteRepository battleVoteRepository;
  private final BattleNotificationSentRepository sentRepository;
  private final NotificationService notificationService;

  /**
   * 배틀 D-7 생성자 알림 — 매시간 정각.
   *
   * <p>윈도우 {@code [now+7d, now+7d+1h)}는 cron 주기(1h)와 폭을 맞춰
   * 같은 배틀을 중복으로 잡거나 놓치지 않게 한다.
   *
   * <p>D-7 시점은 즉시 액션 유도가 약해 push 피로도만 늘리므로 채널은
   * in-app만 사용한다. 이미 발송된 {@code (battleId, creatorId)} 쌍은
   * {@link BattleNotificationSent}로 dedup하여 cron이 다시 돌아도 idempotent하다.
   */
  @Scheduled(cron = "0 0 * * * *")
  @SchedulerLock(name = "battleDeadlineD7", lockAtLeastFor = "1m", lockAtMostFor = "10m")
  @Transactional
  public void notifyD7() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime from = now.plusDays(7);
    LocalDateTime to = from.plusHours(1);

    List<Battle> battles = battleRepository.findByEndDateBetween(from, to);
    if (battles.isEmpty()) {
      log.debug("[배틀 알림] D-7 대상 없음");
      return;
    }

    Set<String> alreadySent = loadAlreadySentKeys(battles, BattleNotificationPhase.D7);

    List<BattleNotificationSent> newSent = battles.stream()
        .filter(b -> !alreadySent.contains(sentKey(b.getId(), b.getCreatorId())))
        .map(this::sendD7AndMark)
        .toList();

    sentRepository.saveAll(newSent);
    log.info("[배틀 알림] D-7 발송 완료 - 대상 배틀 {}건, 발송 {}건", battles.size(), newSent.size());
  }

  /**
   * 배틀 종료 알림 — 매분.
   *
   * <p>윈도우 {@code [now-1m, now)}는 cron 주기(1m)와 일치. 사용자 체감상
   * "종료 시점"에 발송되도록 분 단위로 자주 돌며, 더 짧은 주기는 부하만
   * 늘리고 체감 차이는 없다.
   *
   * <p>대상은 생성자 + 투표자(로그인 유저만, 게스트 제외). 생성자가 본인
   * 배틀에 투표한 경우 {@code Set}으로 1건 dedup. {@link BattleNotificationSent}로
   * {@code (battleId, userId)} 단위 idempotent 보장.
   */
  @Scheduled(cron = "0 * * * * *")
  @SchedulerLock(name = "battleDeadlineEnd", lockAtLeastFor = "30s", lockAtMostFor = "5m")
  @Transactional
  public void notifyEnd() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime from = now.minusMinutes(1);

    List<Battle> battles = battleRepository.findByEndDateBetween(from, now);
    if (battles.isEmpty()) {
      log.debug("[배틀 알림] 종료 대상 없음");
      return;
    }

    Set<String> alreadySent = loadAlreadySentKeys(battles, BattleNotificationPhase.END);

    List<BattleNotificationSent> newSent = battles.stream()
        .flatMap(battle -> resolveEndRecipients(battle, alreadySent).stream()
            .map(userId -> sendResultAndMark(battle, userId)))
        .toList();

    sentRepository.saveAll(newSent);
    log.info("[배틀 알림] 종료 발송 완료 - 대상 배틀 {}건, 발송 {}건", battles.size(), newSent.size());
  }

  private BattleNotificationSent sendD7AndMark(Battle battle) {
    notificationService.notifyBattleDeadlineOwnerD7(
        battle.getCreatorId(), battle.getId(), battle.getTitle());
    return BattleNotificationSent.of(battle.getId(), BattleNotificationPhase.D7, battle.getCreatorId());
  }

  private BattleNotificationSent sendResultAndMark(Battle battle, Long userId) {
    notificationService.notifyBattleResult(userId, battle.getId(), battle.getTitle());
    return BattleNotificationSent.of(battle.getId(), BattleNotificationPhase.END, userId);
  }

  /** 생성자 + 투표자 중 미발송 유저만 추린다. 게스트(null)도 제외. */
  private Set<Long> resolveEndRecipients(Battle battle, Set<String> alreadySent) {
    Set<Long> recipients = new HashSet<>();
    recipients.add(battle.getCreatorId());
    recipients.addAll(battleVoteRepository.findDistinctVoterUserIdsByBattleId(battle.getId()));
    recipients.removeIf(userId -> userId == null
        || alreadySent.contains(sentKey(battle.getId(), userId)));
    return recipients;
  }

  /** 이미 발송된 {@code (battleId, userId)} 쌍을 한 번의 IN 쿼리로 조회해 lookup용 셋으로. */
  private Set<String> loadAlreadySentKeys(List<Battle> battles, BattleNotificationPhase phase) {
    return sentRepository
        .findByBattleIdInAndPhase(
            battles.stream().map(Battle::getId).toList(), phase)
        .stream()
        .map(s -> sentKey(s.getBattleId(), s.getUserId()))
        .collect(Collectors.toSet());
  }

  private static String sentKey(Long battleId, Long userId) {
    return battleId + ":" + userId;
  }
}
