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
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import com.example.toycontent.app.common.utils.YoutubeUtils;
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
    Battle battle = getBattleById(battleId);
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
   * 배틀 아이템 생성 (즉시 활성 상태로 등록)
   * - CREATOR_ONLY, PUBLIC 권한의 배틀에서 사용
   */
  @Transactional
  public void createBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    saveBattleItems(userId, battle, request, BattleItemStatus.ACTIVE);
  }

  /**
   * 배틀 아이템 승인 요청 (검토 대기 상태로 등록)
   * - PUBLIC_APPROVAL 권한의 배틀에서 사용
   * - 배틀 생성자가 승인해야 활성화됨
   */
  @Transactional
  public void requestAddBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    saveBattleItems(userId, battle, request, BattleItemStatus.UNDER_REVIEW);
  }

  private void saveBattleItems(Long userId, Battle battle, List<ItemRequest> request,
      BattleItemStatus status) {
    List<BattleItem> battleItems = request.stream()
        .map(itemRequest -> createBattleItem(userId, battle, itemRequest, status))
        .toList();

    battleItemRepository.saveAll(battleItems);
  }

  /**
   * 타입별로 필요한 필드만 세팅하여 BattleItem을 생성한다.
   * - PRODUCT: 제품 조회 후 연결 (존재하지 않으면 예외)
   * - CUSTOM: 사용자가 직접 입력한 제품 정보 세팅
   * - YOUTUBE: URL에서 videoId를 추출하여 저장 (유효하지 않은 URL이면 예외)
   */
  private BattleItem createBattleItem(Long userId, Battle battle, ItemRequest request,
      BattleItemStatus status) {
    BattleItem.BattleItemBuilder builder = BattleItem.builder()
        .battle(battle)
        .registerId(userId)
        .itemType(request.getItemType())
        .status(status);

    switch (request.getItemType()) {
      case PRODUCT -> {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RestApiException(ProductErrorCode.PRODUCT_NOT_FOUND));
        builder.product(product);
      }
      case CUSTOM -> builder
          .customName(request.getCustomName())
          .customBrand(request.getCustomBrand())
          .customImageUrl(request.getCustomImageUrl());
      case YOUTUBE -> {
        String videoId = YoutubeUtils.extractVideoId(request.getContentUrl());
        builder
            .customName(request.getCustomName())
            .customBrand(request.getCustomBrand())
            .contentUrl(request.getContentUrl())
            .contentId(videoId);
      }
    }

    return builder.build();
  }

  /**
   * 배틀 아이템 승인
   */
  @Transactional
  public void approveBattleItem(Long battleId, Long itemId, Long userId) {
    Battle battle = getBattleById(battleId);
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
    Battle battle = getBattleById(battleId);
    validateBattleCreator(battle, userId);

    BattleItem item = battleItemRepository.findById(itemId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

    if (!item.getBattle().getId().equals(battleId)) {
      throw new RestApiException(BattleErrorCode.INVALID_BATTLE_ITEM);
    }

    item.exclude();
  }

  /**
   * 배틀 아이템에 투표한다.
   * - 단일 투표(SINGLE): 하나의 아이템에만 투표 가능, 재투표 불가
   * - 복수 투표(MULTIPLE): 여러 아이템에 투표 가능, 재투표 시 기존 투표를 삭제하고 새로 반영
   */
  @Transactional
  public void vote(Long battleId, Long currentUserId, BattleVoteRequest.Vote request) {
    Battle battle = getBattleById(battleId);
    List<BattleVoteRequest.VoteItem> voteItems = request.getVotes();
    List<BattleVote> existingVotes = battleVoteRepository.findByBattle_IdAndUserId(battleId, currentUserId);

    if (battle.isSingleVote()) {
      validateSingleVote(voteItems, existingVotes.size());
    } else {
      validateMultipleVote(voteItems);
      removeExistingVotesIfPresent(battle, existingVotes);
    }

    List<BattleVote> newVotes = createVotes(battle, currentUserId, voteItems);
    battleVoteRepository.saveAll(newVotes);
    applyVoteStatistics(battle, newVotes);
  }

  /**
   * 복수 투표 시, 기존 투표가 있으면 통계를 되돌리고 삭제한다.
   * 단일 투표는 재투표 자체가 불가능하므로 이 메서드를 호출하지 않는다.
   */
  private void removeExistingVotesIfPresent(Battle battle, List<BattleVote> existingVotes) {
    if (existingVotes.isEmpty()) {
      return;
    }

    rollbackVoteStatistics(battle, existingVotes);
    battle.getVotes().removeAll(existingVotes);
    battleVoteRepository.deleteAll(existingVotes);
  }

  /**
   * 새로운 투표를 통계에 반영한다.
   * - 참여자 수 증가
   * - 총 투표 수 증가 (단일: 한 표, 복수: 투표한 아이템 수만큼)
   * - 각 아이템의 득표 수와 점수 증가
   */
  private void applyVoteStatistics(Battle battle, List<BattleVote> votes) {
    battle.incrementTotalParticipants();

    int voteCount = battle.isSingleVote() ? 1 : votes.size();
    battle.addTotalVotes(voteCount);

    votes.forEach(vote -> {
      BattleItem item = vote.getBattleItem();
      int score = calculateScore(battle.getVoteType(), vote.getVoteRank());

      item.incrementVoteCount();
      item.addScore(score);
      battle.addTotalScore(score);
    });
  }

  /**
   * 기존 투표를 통계에서 되돌린다.
   * applyVoteStatistics의 역연산으로, 복수 투표 재투표 시 사용된다.
   */
  private void rollbackVoteStatistics(Battle battle, List<BattleVote> votes) {
    battle.decrementTotalParticipants();

    int voteCount = battle.isSingleVote() ? 1 : votes.size();
    battle.subtractTotalVotes(voteCount);

    votes.forEach(vote -> {
      BattleItem item = vote.getBattleItem();
      int score = calculateScore(battle.getVoteType(), vote.getVoteRank());

      item.decrementVoteCount();
      item.subtractScore(score);
      battle.subtractTotalScore(score);
    });
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
    Battle battle = getBattleById(battleId);

    List<BattleVote> votes = battleVoteRepository.findByBattleAndUserId(battle, userId);

    if (votes.isEmpty()) {
      throw new RestApiException(BattleErrorCode.VOTE_NOT_FOUND);
    }

    rollbackVoteStatistics(battle, votes);

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
    Battle battle = getBattleById(battleId);

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


  private Battle getBattleById(Long battleId) {
    return battleRepository.findById(battleId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_NOT_FOUND));
  }

  private void validateBattleCreator(Battle battle, Long userId) {
    if (!battle.getCreatorId().equals(userId)) {
      throw new RestApiException(BattleErrorCode.NOT_BATTLE_CREATOR);
    }
  }

}