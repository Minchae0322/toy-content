package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest.ItemRequest;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import java.util.List;
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
          Product product = productRepository.findById(itemRequest.getProductId())
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

    // 해당 아이템에 투표한 모든 투표 무효 처리
    List<BattleVote> votes = battleVoteRepository.findByBattleItemAndIsDeletedFalse(item);
    votes.forEach(BattleVote::softDelete);
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