package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest.ItemRequest;
import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleAttachmentFile;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleAttachmentFileRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.common.exception.impl.CategoryErrorCode;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.UserCacheService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleService {

  private final BattleRepository battleRepository;
  private final BattleItemRepository battleItemRepository;
  private final BattleVoteRepository battleVoteRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final UserCacheService userCacheService;
  private final BattleAttachmentFileRepository battleAttachmentFileRepository;


  private static final int REQUIRED_LEVEL = 5;
  private static final int MAX_ACTIVE_BATTLES = 5;
  private static final int MAX_DAILY_CREATIONS = 3;
  private static final int MIN_ITEMS = 2;
  private static final int MAX_ITEMS = 20;
  private static final int MAX_ADDITIONAL_ITEMS = 3;
  private static final int AUTO_REVIEW_REPORT_COUNT = 3;

  public void validateCreation(Long userId) {
    // 동시 진행 배틀 수 체크
    long activeCount = battleRepository.countByCreatorIdAndStatus(userId, BattleStatus.ACTIVE);
    if (activeCount >= MAX_ACTIVE_BATTLES) {
      throw new RestApiException(BattleErrorCode.MAX_ACTIVE_BATTLES);
    }

    // 24시간 내 생성 횟수 체크
    LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
    long dailyCount = battleRepository.countByCreatorIdAndCreatedAtAfter(userId, dayAgo);
    if (dailyCount >= MAX_DAILY_CREATIONS) {
      throw new RestApiException(BattleErrorCode.DAILY_LIMIT_EXCEEDED);
    }
  }

  /**
   * 배틀 생성
   */
  @Transactional
  public BattleResponse.BattleCreateResponse createBattle(Long userId, BattleRequest.CreateBattle request) {
    validateCreation(userId);
    validateBattlePeriod(request.getStartDate(), request.getEndDate());

    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

    Battle battle = createBattleEntity(userId, request, category);
    battleRepository.save(battle);

    createBattleAttachmentFiles(request.getThumbnailAttachmentInfo(), battle);

    createBattleItems(userId, battle, request.getItems());

    return BattleResponse.BattleCreateResponse.from(battle);
  }


  /**
   * 배틀 목록 조회
   */
  public Page<BattleResponse.BattleList> getBattles(BattleSearchCondition condition, Pageable pageable) {
    List<BattleList> battleLists = battleRepository.findBattlesWithSearchCondition(condition,
        pageable);

    battleLists.forEach(
        battle -> battle.setCreatorUserInfo(userCacheService.getUserInfo(battle.getCreatorId())));

    Long totalCount = battleRepository.countBattlesWithSearchCondition(condition);

    return new PageImpl<>(battleLists, pageable, totalCount);
  }

  /**
   * 배틀 기간 검증
   */
  private void validateBattlePeriod(LocalDateTime startDate, LocalDateTime endDate) {
    long days = ChronoUnit.DAYS.between(startDate, endDate);
    
    //우선은 기한 제한 없이
    /*if (days < 7 || days > 31) {
      throw new RestApiException(BattleErrorCode.INVALID_BATTLE_PERIOD);
    }*/
  }

  /**
   * 배틀 엔티티 생성
   */
  private Battle createBattleEntity(Long userId, BattleRequest.CreateBattle request, Category category) {
    return Battle.builder()
        .title(request.getTitle())
        .description(request.getDescription())
        .category(category)
        .creatorId(userId)
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .participationStartDate(request.getParticipationStartDate())
        .voteType(request.getVoteType())
        .allowDuplicateProducts(request.getAllowDuplicateProducts())
        .status(BattleStatus.ACTIVE)
        .build();
  }


  /**
   * 배틀 아이템 생성 및 저장
   */
  private void createBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    List<BattleItem> battleItems = request.stream()
        .map(itemRequest -> {
          Product product = productRepository.findById(itemRequest.getProductId())
              .orElse(null);

          return BattleItem.builder()
              .battle(battle)
              .product(product)
              .registerId(userId)
              .customName(itemRequest.getCustomName())
              .customBrand(itemRequest.getCustomBrand())
              .customImageUrl(itemRequest.getCustomImageUrl())
              .build();
        })
        .toList();

    battleItemRepository.saveAll(battleItems);
  }

  /**
   * 제품 첨부파일(대표 이미지 + 상세 이미지) 생성
   * - 썸네일(대표 이미지)와 상세 이미지 파일을 각각 엔티티로 변환 후 일괄 저장
   */
  private void createBattleAttachmentFiles(AttachmentInfo thumbnailAttachmentInfo,
      Battle battle) {
    // 대표 이미지 파일 생성
    BattleAttachmentFile primaryImage = createAttachmentFile(thumbnailAttachmentInfo, battle, 0, true);

    // 대표 이미지 저장
    battleAttachmentFileRepository.save(primaryImage);
  }

  /**
   * 개별 첨부파일 생성 헬퍼 메서드
   * - AttachmentInfo → ProductAttachmentFile 변환
   * - 순서(order)와 대표 여부(isPrimary) 설정 포함
   */
  private BattleAttachmentFile createAttachmentFile(AttachmentInfo info, Battle battle, int order, boolean isPrimary) {
    return info.toEntity(battle, order, isPrimary);
  }


  /**
   * 배틀 상세 조회
   */
  @Transactional
  public BattleResponse.BattleDetail getBattleDetail(Long battleId, Long currentUserId) {
    Battle battle = getBattleById(battleId);
    ExternalUserInfo userInfo = userCacheService.getUserInfo(battle.getCreatorId());

    boolean isCreator = Optional.ofNullable(currentUserId)
        .filter(battle.getCreatorId()::equals)
        .isPresent();

    boolean isHasVoted = Optional.ofNullable(currentUserId)
        .map(userId -> battleVoteRepository.existsByBattleIdAndUserId(battleId, userId))
        .orElse(false);

    battle.incrementViews();

    return BattleResponse.BattleDetail.from(battle, userInfo, isCreator, isHasVoted);
  }


  /**
   * 배틀 아이템 추가 (큐레이션 배틀, 진행 중 추가)
   */
  @Transactional
  public void addBattleItems(Long battleId, Long userId, BattleRequest.AddBattleItems request) {
    Battle battle = getBattleById(battleId);
    // 생성자 권한 확인
    if (ItemAddPermissionType.CREATOR_ONLY.equals(battle.getItemAddPermissionType())) {
      validateBattleCreator(battle, userId);
    }

    // 최대 3개까지만 추가 가능
    if (request.getItems().size() > MAX_ADDITIONAL_ITEMS) {
      throw new RestApiException(BattleErrorCode.TOO_MANY_ITEMS);
    }

    // 현재 아이템 수 확인
    long currentCount = battleItemRepository.countByBattleAndIsDeletedFalse(battle);
    if (currentCount + request.getItems().size() > MAX_ITEMS) {
      throw new RestApiException(BattleErrorCode.MAX_ITEMS_EXCEEDED);
    }

    createBattleItems(userId, battle, request.getItems());

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

    // 아이템 제외 처리
    item.exclude();

    // 해당 아이템에 투표한 모든 투표 무효 처리
    List<BattleVote> votes = battleVoteRepository.findByBattleItemAndIsDeletedFalse(item);
    votes.forEach(BattleVote::softDelete);

    // 투표 수 재계산
    // battle.recalculateVotes();
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

    log.info("배틀 아이템 승인: battleId={}, itemId={}", battleId, itemId);
  }

  /**
   * 배틀 투표
   */
  @Transactional
  public void vote(Long battleId, Long userId, BattleRequest.Vote request) {
    Battle battle = getBattleById(battleId);

    // 배틀 상태 확인
    if (battle.getStatus() != BattleStatus.ACTIVE) {
      throw new RestApiException(BattleErrorCode.BATTLE_NOT_ACTIVE);
    }

    // 투표 기간 확인
    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(battle.getStartDate()) || now.isAfter(battle.getEndDate())) {
      throw new RestApiException(BattleErrorCode.INVALID_VOTE_PERIOD);
    }

    // 기존 투표 확인 및 삭제
    List<BattleVote> existingVotes = battleVoteRepository
        .findByBattleAndUserIdAndIsDeletedFalse(battle, userId);
    existingVotes.forEach(BattleVote::softDelete);

    // 새로운 투표 생성
    createVotes(battle, userId, request);

    // 배틀 통계 업데이트
    // battle.incrementTotalVotes();

    // 투표 리워드 지급 (+5 EXP)
    // rewardService.rewardBattleVote(userId);

    log.info("배틀 투표 완료: battleId={}, userId={}, voteCount={}",
        battleId, userId, request.getVotes().size());
  }

  /**
   * 배틀 투표 취소
   */
  @Transactional
  public void cancelVote(Long battleId, Long userId) {
    Battle battle = getBattleById(battleId);

    List<BattleVote> votes = battleVoteRepository
        .findByBattleAndUserIdAndIsDeletedFalse(battle, userId);

    if (votes.isEmpty()) {
      throw new RestApiException(BattleErrorCode.VOTE_NOT_FOUND);
    }

    votes.forEach(vote -> {
      vote.softDelete();
     // vote.getBattleItem().decrementVote();
    });

    log.info("배틀 투표 취소: battleId={}, userId={}, canceledCount={}",
        battleId, userId, votes.size());
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

  // ========== Private Helper Methods ==========

  private Battle getBattleById(Long battleId) {
    return battleRepository.findById(battleId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_NOT_FOUND));
  }

  private void validateBattleCreator(Battle battle, Long userId) {
    if (!battle.getCreatorId().equals(userId)) {
      throw new RestApiException(BattleErrorCode.NOT_BATTLE_CREATOR);
    }
  }

  private void addInitialItems(Battle battle, List<BattleRequest.ItemRequest> items) {
    for (int i = 0; i < items.size(); i++) {
      BattleRequest.ItemRequest itemReq = items.get(i);
      BattleItem item = createBattleItem(battle, itemReq, i);
      battleItemRepository.save(item);
    }
  }



  private BattleItem createBattleItem(Battle battle, BattleRequest.ItemRequest request, int order) {
    BattleItem.BattleItemBuilder builder = BattleItem.builder()
        .battle(battle)
        .displayOrder(order)
        .status(BattleItemStatus.ACTIVE);

    if (request.getProductId() != null) {
      // 기존 제품 선택
      // Product product = productService.getProductById(request.getProductId());
      // builder.product(product);
    } else {
      // 커스텀 아이템
      builder.customName(request.getCustomName())
          .customBrand(request.getCustomBrand())
          .customImageUrl(request.getCustomImageUrl());
    }

    return builder.build();
  }

  private void createVotes(Battle battle, Long userId, BattleRequest.Vote request) {
    for (BattleRequest.VoteItem voteReq : request.getVotes()) {
      BattleItem item = battleItemRepository.findById(voteReq.getItemId())
          .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_ITEM_NOT_FOUND));

      // 투표 가능 상태 확인
      if (!item.canVote()) {
        throw new RestApiException(BattleErrorCode.CANNOT_VOTE_ITEM);
      }

      // 투표 생성
      BattleVote vote = BattleVote.builder()
          .battle(battle)
          .userId(userId)
          .battleItem(item)
          .rank(voteReq.getRank())
          .score(calculateScore(voteReq.getRank()))
          .build();

      battleVoteRepository.save(vote);

      // 아이템 투표 수 증가
      item.incrementVote();
    }
  }

  private int calculateScore(int rank) {
    return switch (rank) {
      case 1 -> 3;
      case 2 -> 2;
      case 3 -> 1;
      default -> 1;
    };
  }
}