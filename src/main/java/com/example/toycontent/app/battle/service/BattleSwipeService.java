package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.controller.dto.BattleSwipeRequest;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.NextItem;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.NextItems;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.Result;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.ResultItem;
import com.example.toycontent.app.battle.controller.dto.BattleSwipeResponse.SwipeAck;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleSwipe;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleSwipeRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.SwipeVerdict;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.common.voter.VoterId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스와이프 배틀 처리 서비스.
 *
 * <ul>
 *   <li>1회 스와이프 = 1건 저장 (변경 불가, 중복 시 SWIPE_ALREADY_DONE)</li>
 *   <li>미진행 = 해당 voter의 BattleSwipe row가 없는 활성 아이템</li>
 *   <li>랭킹 = {@code BattleItem.getSwipeRankingScore()} 내림차순</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleSwipeService {

  private final BattleRepository battleRepository;
  private final BattleItemRepository battleItemRepository;
  private final BattleSwipeRepository battleSwipeRepository;

  /**
   * 멱등 덮어쓰기 정책:
   * <ul>
   *   <li>기존 row 없음 → 신규 저장 + 새 verdict 카운터 +1</li>
   *   <li>기존 row 있고 동일 verdict → no-op (응답만 정상 반환)</li>
   *   <li>기존 row 있고 다른 verdict → 기존 verdict 카운터 -1, 새 verdict 카운터 +1, row의 verdict 갱신</li>
   * </ul>
   * 어떤 경로로 같은 (battle, item, voter) 조합이 재호출되어도 카운트는 마지막 verdict만 반영된다.
   */
  @Transactional
  public SwipeAck swipe(Long battleId, VoterId voter, BattleSwipeRequest.Swipe request) {
    Battle battle = loadSwipeBattle(battleId);
    BattleItem item = loadActiveItem(battle, request.getItemId());
    SwipeVerdict newVerdict = request.getVerdict();

    Optional<BattleSwipe> existing = findExistingSwipe(battleId, item.getId(), voter);
    if (existing.isPresent()) {
      applyOverwrite(item, existing.get(), newVerdict);
    } else {
      applyNew(battle, item, voter, newVerdict);
    }

    return buildAck(battle, voter, item.getId());
  }

  private BattleItem loadActiveItem(Battle battle, Long itemId) {
    BattleItem item = battleItemRepository.findById(itemId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));
    if (!item.getBattle().getId().equals(battle.getId())) {
      throw new RestApiException(BattleErrorCode.INVALID_BATTLE_ITEM);
    }
    if (item.getStatus() != BattleItemStatus.ACTIVE || Boolean.TRUE.equals(item.getIsDeleted())) {
      throw new RestApiException(BattleErrorCode.CANNOT_VOTE_ITEM);
    }
    return item;
  }

  private Optional<BattleSwipe> findExistingSwipe(Long battleId, Long itemId, VoterId voter) {
    return voter.isUser()
        ? battleSwipeRepository.findByBattle_IdAndBattleItem_IdAndUserId(
            battleId, itemId, voter.userId())
        : battleSwipeRepository.findByBattle_IdAndBattleItem_IdAndGuestId(
            battleId, itemId, voter.guestId());
  }

  private void applyNew(Battle battle, BattleItem item, VoterId voter, SwipeVerdict verdict) {
    BattleSwipe swipe = voter.isUser()
        ? BattleSwipe.ofUser(battle, item, voter.userId(), verdict)
        : BattleSwipe.ofGuest(battle, item, voter.guestId(), verdict);
    battleSwipeRepository.save(swipe);
    incrementCounter(item, verdict);
  }

  private void applyOverwrite(BattleItem item, BattleSwipe existing, SwipeVerdict newVerdict) {
    if (existing.getVerdict() == newVerdict) {
      return; // 동일 verdict — 멱등 no-op
    }
    decrementCounter(item, existing.getVerdict());
    incrementCounter(item, newVerdict);
    existing.changeVerdict(newVerdict);
  }

  private SwipeAck buildAck(Battle battle, VoterId voter, Long itemId) {
    return SwipeAck.builder()
        .itemId(itemId)
        .completedCount(countSwipedItems(battle.getId(), voter))
        .totalCount(countActiveItems(battle))
        .build();
  }

  public NextItems findNextItems(Long battleId, VoterId voter, int size) {
    Battle battle = loadSwipeBattle(battleId);
    List<BattleItem> active = activeItems(battle);
    Set<Long> swipedIds = new HashSet<>(swipedItemIds(battleId, voter));

    List<BattleItem> pending = new ArrayList<>(active.stream()
        .filter(i -> !swipedIds.contains(i.getId()))
        .toList());
    Collections.shuffle(pending);

    List<NextItem> nextItems = pending.stream()
        .limit(Math.max(size, 1))
        .map(NextItem::from)
        .toList();

    return NextItems.builder()
        .items(nextItems)
        .completedCount(swipedIds.size())
        .totalCount(active.size())
        .build();
  }

  public Result getResult(Long battleId) {
    Battle battle = loadSwipeBattle(battleId);
    List<BattleItem> sorted = activeItems(battle).stream()
        .sorted(Comparator
            .comparingInt(BattleItem::getSwipeRankingScore).reversed()
            .thenComparing(BattleItem::getId))
        .toList();

    List<ResultItem> items = new ArrayList<>(sorted.size());
    for (int i = 0; i < sorted.size(); i++) {
      BattleItem item = sorted.get(i);
      items.add(ResultItem.builder()
          .rank(i + 1)
          .id(item.getId())
          .displayName(item.getDisplayName())
          .imageUrl(item.getDisplayImageUrl())
          .strongPickCount(item.getStrongPickCount())
          .pickCount(item.getPickCount())
          .passCount(item.getPassCount())
          .score(item.getSwipeRankingScore())
          .build());
    }
    return Result.builder().items(items).build();
  }

  private Battle loadSwipeBattle(Long battleId) {
    Battle battle = battleRepository.findById(battleId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_NOT_FOUND));
    if (battle.getVoteType() != VoteType.SWIPE) {
      throw new RestApiException(BattleErrorCode.SWIPE_NOT_ALLOWED);
    }
    return battle;
  }

  private List<Long> swipedItemIds(Long battleId, VoterId voter) {
    return voter.isUser()
        ? battleSwipeRepository.findSwipedItemIdsByUser(battleId, voter.userId())
        : battleSwipeRepository.findSwipedItemIdsByGuest(battleId, voter.guestId());
  }

  private int countSwipedItems(Long battleId, VoterId voter) {
    return swipedItemIds(battleId, voter).size();
  }

  private List<BattleItem> activeItems(Battle battle) {
    return battle.getItems().stream()
        .filter(i -> i.getStatus() == BattleItemStatus.ACTIVE
            && !Boolean.TRUE.equals(i.getIsDeleted()))
        .toList();
  }

  private int countActiveItems(Battle battle) {
    return activeItems(battle).size();
  }

  private static void incrementCounter(BattleItem item, SwipeVerdict verdict) {
    switch (verdict) {
      case STRONG_PICK -> item.incrementStrongPickCount();
      case PICK -> item.incrementPickCount();
      case PASS -> item.incrementPassCount();
    }
  }

  private static void decrementCounter(BattleItem item, SwipeVerdict verdict) {
    switch (verdict) {
      case STRONG_PICK -> item.decrementStrongPickCount();
      case PICK -> item.decrementPickCount();
      case PASS -> item.decrementPassCount();
    }
  }
}
