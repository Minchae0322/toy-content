package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest.ItemRequest;
import com.example.toycontent.app.battle.controller.dto.BattleVoteRequest;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleItemService {

  private final BattleRepository battleRepository;
  private final BattleItemRepository battleItemRepository;
  private final BattleVoteRepository battleVoteRepository;
  private final ProductRepository productRepository;

  private static final int MAX_ITEMS = 20;
  private static final int MAX_ADDITIONAL_ITEMS = 3;
  private static final int AUTO_REVIEW_REPORT_COUNT = 3;

  /**
   * 배틀 아이템 추가
   */
  @Transactional
  public void addBattleItems(Long battleId, Long userId, BattleRequest.AddBattleItems request) {
    Battle battle = getBattleByIdOrElseThrow(battleId);
    List<ItemRequest> items = request.getItems();

    validateItemAddition(battle, userId, items);
    addItemsByPermission(battle, userId, items);
  }

  private void validateItemAddition(Battle battle, Long userId, List<ItemRequest> items) {
    // 생성자 권한 확인
    if (battle.getItemAddPermissionType() == ItemAddPermissionType.CREATOR_ONLY) {
      validateBattleCreator(battle, userId);
    }

    // 최대 3개까지만 추가 가능
    if (items.size() > MAX_ADDITIONAL_ITEMS) {
      throw new RestApiException(BattleErrorCode.TOO_MANY_ITEMS);
    }

    // 현재 아이템 수 확인
    long currentCount = battleItemRepository.countByBattleAndIsDeletedFalse(battle);
    if (currentCount + items.size() > MAX_ITEMS) {
      throw new RestApiException(BattleErrorCode.MAX_ITEMS_EXCEEDED);
    }
  }

  private void addItemsByPermission(Battle battle, Long userId, List<ItemRequest> items) {
    ItemAddPermissionType permission = battle.getItemAddPermissionType();

    if (permission == ItemAddPermissionType.PUBLIC_APPROVAL) {
      requestAddBattleItems(userId, battle, items);
    } else {
      createBattleItems(userId, battle, items);
    }
  }

  /**
   * 배틀 아이템 생성 및 저장
   */
  public void createBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    List<BattleItem> battleItems = request.stream()
        .map(itemRequest -> {
          Product product = Optional.ofNullable(itemRequest.getProductId())
              .flatMap(productRepository::findById)
              .orElse(null);

          return BattleItem.builder()
              .battle(battle)
              .product(product)
              .registerId(userId)
              .status(BattleItemStatus.ACTIVE)
              .customName(itemRequest.getCustomName())
              .customBrand(itemRequest.getCustomBrand())
              .customImageUrl(itemRequest.getCustomImageUrl())
              .build();
        })
        .toList();

    battleItemRepository.saveAll(battleItems);
  }

  public void requestAddBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    List<BattleItem> battleItems = request.stream()
        .map(itemRequest -> {
          Product product = productRepository.findById(itemRequest.getProductId())
              .orElse(null);

          return BattleItem.builder()
              .battle(battle)
              .product(product)
              .registerId(userId)
              .status(BattleItemStatus.UNDER_REVIEW)
              .customName(itemRequest.getCustomName())
              .customBrand(itemRequest.getCustomBrand())
              .customImageUrl(itemRequest.getCustomImageUrl())
              .build();
        })
        .toList();

    battleItemRepository.saveAll(battleItems);
  }

  /**
   * 배틀 아이템 승인
   */
  @Transactional
  public void approveBattleItem(Long battleId, Long itemId, Long userId) {
    Battle battle = getBattleByIdOrElseThrow(battleId);
    validateBattleCreator(battle, userId);

    BattleItem item = battleItemRepository.findById(itemId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

    if (!item.getBattle().getId().equals(battleId)) {
      throw new RestApiException(BattleErrorCode.INVALID_BATTLE_ITEM);
    }

    // 검토 중 상태가 아니면 승인 불가
    if (item.getStatus() != BattleItemStatus.UNDER_REVIEW) {
      throw new RestApiException(BattleErrorCode.INVALID_ITEM_STATUS);
    }

    item.approve();
  }

  /**
   * 배틀 아이템 제외 처리
   */
  @Transactional
  public void excludeBattleItem(Long battleId, Long itemId, Long userId) {
    Battle battle = getBattleByIdOrElseThrow(battleId);
    validateBattleCreator(battle, userId);

    BattleItem item = battleItemRepository.findById(itemId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

    if (!item.getBattle().getId().equals(battleId)) {
      throw new RestApiException(BattleErrorCode.INVALID_BATTLE_ITEM);
    }

    item.exclude();
  }

  /**
   * 배틀 아이템 투표
   */
  @Transactional
  public void vote(Long battleId, Long currentUserId, BattleVoteRequest.Vote request) {
    Battle battle = getBattleByIdOrElseThrow(battleId);

    validateAndHandleExistingVotes(battle, currentUserId, request);

    List<BattleVote> votes = createVotes(battle, currentUserId, request.getVotes());
    battleVoteRepository.saveAll(votes);

    updateVoteStatistics(battle, votes, true);
  }

  /**
   * 투표 검증 및 기존 투표 처리
   */
  private void validateAndHandleExistingVotes(Battle battle, Long userId, BattleVoteRequest.Vote request) {
    List<BattleVoteRequest.VoteItem> voteItems = request.getVotes();
    List<BattleVote> existingVotes = battleVoteRepository.findByBattle_IdAndUserId(battle.getId(),
        userId);

    if (VoteType.SINGLE.equals(battle.getVoteType())) {
      validateSingleVote(voteItems, existingVotes.size());
      return;
    }

    // MULTIPLE 타입
    validateMultipleVote(voteItems);
    handleExistingVotesIfPresent(battle, existingVotes);
  }

  /**
   * 1인 1표 검증
   */
  private void validateSingleVote(List<BattleVoteRequest.VoteItem> voteItems, int existingVoteCount) {
    if (existingVoteCount > 0) {
      throw new RestApiException(BattleErrorCode.ALREADY_VOTED);
    }

    if (voteItems.size() != 1 || voteItems.get(0).getRank() != 1) {
      throw new RestApiException(BattleErrorCode.INVALID_VOTE_COUNT);
    }
  }

  /**
   * 1인 3표 검증
   */
  private void validateMultipleVote(List<BattleVoteRequest.VoteItem> voteItems) {
    List<Integer> ranks = voteItems.stream()
        .map(BattleVoteRequest.VoteItem::getRank)
        .sorted()
        .toList();

    for (int i = 0; i < ranks.size(); i++) {
      if (ranks.get(i) != i + 1) {
        throw new RestApiException(BattleErrorCode.INVALID_RANK_SEQUENCE);
      }
    }
  }

  /**
   * 기존 투표 처리 (수정 로직)
   */
  private void handleExistingVotesIfPresent(Battle battle,  List<BattleVote> existingVotes) {
    if (existingVotes.isEmpty()) {
      return;
    }

    updateVoteStatistics(battle, existingVotes, false);

    battle.getVotes().removeAll(existingVotes);
    battleVoteRepository.deleteAll(existingVotes);

  }

  /**
   * 투표 통계 업데이트
   */
  private void updateVoteStatistics(Battle battle, List<BattleVote> votes, boolean isAdd) {
    int delta = isAdd ? 1 : -1;

    battle.incrementTotalParticipants(delta);

    int voteCountDelta = (battle.getVoteType() == VoteType.SINGLE)
        ? delta
        : votes.size() * delta;
    battle.incrementTotalVotes(voteCountDelta);

    // voteCount 및 totalScore 업데이트
    votes.forEach(vote -> {
      BattleItem item = vote.getBattleItem();
      item.incrementVoteCount(delta);

      int score = calculateScore(battle.getVoteType(), vote.getVoteRank());
      if (isAdd) {
        item.addScore(score);
      } else {
        item.subtractScore(score);
      }
    });
  }

  /**
   * 투표 타입과 순위에 따른 점수 계산
   * - SINGLE: 1점
   * - MULTIPLE: 1위=3점, 2위=2점, 3위=1점
   */
  private int calculateScore(VoteType voteType, Integer rank) {
    if (voteType == VoteType.SINGLE) {
      return 1;
    }

    // MULTIPLE 타입
    return switch (rank) {
      case 1 -> 3;
      case 2 -> 2;
      case 3 -> 1;
      default -> 0;
    };
  }





  /**
   * 배틀 아이템 투표 취소
   */
  @Transactional
  public void cancelVote(Long battleId, Long userId) {
    Battle battle = getBattleByIdOrElseThrow(battleId);

    List<BattleVote> votes = battleVoteRepository.findByBattleAndUserId(
        battle, userId);

    if (votes.isEmpty()) {
      throw new RestApiException(BattleErrorCode.VOTE_NOT_FOUND);
    }

    // 비정규화 컬럼 업데이트 (취소 전에 먼저)
    updateVoteStatistics(battle, votes, false);

    battle.getVotes().removeAll(votes);
    battleVoteRepository.deleteAll(votes);

  }


  private List<BattleVote> createVotes(Battle battle, Long userId,
      List<BattleVoteRequest.VoteItem> voteItems) {
    return voteItems.stream()
        .map(voteItem -> {
          BattleItem item = battleItemRepository.findById(voteItem.getItemId())
              .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

          // 해당 배틀의 아이템인지 확인
          if (!item.getBattle().getId().equals(battle.getId())) {
            throw new RestApiException(BattleErrorCode.INVALID_BATTLE_ITEM);
          }

          // 활성 상태인지 확인
          if (item.getStatus() != BattleItemStatus.ACTIVE) {
            throw new RestApiException(BattleErrorCode.INVALID_ITEM_STATUS);
          }

          int point = calculateScore(battle.getVoteType(), voteItem.getRank());

          item.incrementVote();
          item.addScore(point);

          return BattleVote.builder()
              .battle(battle)
              .battleItem(item)
              .userId(userId)
              .voteRank(voteItem.getRank())
              .score(point)
              .build();
        })
        .toList();
  }



  /**
   * 배틀 아이템 신고
   */
  @Transactional
  public void reportBattleItem(Long battleId, Long itemId, Long userId, BattleRequest.Report request) {
    Battle battle = getBattleByIdOrElseThrow(battleId);

    BattleItem item = battleItemRepository.findById(itemId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

    /*// 중복 신고 방지
    if (battleItemReportRepository.existsByBattleItemAndReporterId(item, userId)) {
      throw new RestApiException(BattleErrorCode.ALREADY_REPORTED);
    }*/

    // 신고 저장
    // BattleItemReport report = BattleItemReport.builder()
    //     .battleItem(item)
    //     .reporterId(userId)
    //     .reason(request.getReason())
    //     .build();
    // battleItemReportRepository.save(report);

    // 신고 수 증가 (3회 이상 시 자동 검토 중 상태)
    item.incrementReport();

    // 3회 이상 신고 시 생성자에게 알림
    if (item.getReportCount() >= AUTO_REVIEW_REPORT_COUNT) {
      // notificationService.notifyItemUnderReview(battle.getCreatorId(), item);
    }

    log.info("배틀 아이템 신고: battleId={}, itemId={}, reportCount={}",
        battleId, itemId, item.getReportCount());
  }


  private Battle getBattleByIdOrElseThrow(Long battleId) {
    return battleRepository.findById(battleId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_NOT_FOUND));
  }

  private void validateBattleCreator(Battle battle, Long userId) {
    if (!battle.getCreatorId().equals(userId)) {
      throw new RestApiException(BattleErrorCode.NOT_BATTLE_CREATOR);
    }
  }

}