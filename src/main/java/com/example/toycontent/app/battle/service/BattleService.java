package com.example.toycontent.app.battle.service;

import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest.ItemRequest;
import com.example.toycontent.app.battle.controller.dto.BattleResponse;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleItemInfo;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleList;
import com.example.toycontent.app.battle.controller.dto.BattleSearchCondition;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleAttachmentFile;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.repository.BattleAttachmentFileRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.BattleStatus;
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

  private final BattleItemService battleItemService;
  private final BattleRepository battleRepository;
  private final BattleItemRepository battleItemRepository;
  private final BattleVoteRepository battleVoteRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final UserCacheService userCacheService;
  private final BattleAttachmentFileRepository battleAttachmentFileRepository;

  private static final int MAX_ACTIVE_BATTLES = 5;
  private static final int MAX_DAILY_CREATIONS = 3;

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

    battleItemService.createBattleItems(userId, battle, request.getItems());

    return BattleResponse.BattleCreateResponse.from(battle);
  }


  /**
   * 배틀 목록 조회
   */
  public Page<BattleResponse.BattleList> getBattles(BattleSearchCondition condition, Pageable pageable) {
    List<BattleList> battleLists = battleRepository.findBattlesWithSearchCondition(condition, pageable);

    battleLists.forEach(
        battle -> battle.setCreatorUserInfo(userCacheService.getUserInfo(battle.getCreatorId())));

    Long totalCount = battleRepository.countBattlesWithSearchCondition(condition);

    return new PageImpl<>(battleLists, pageable, totalCount);
  }

  /**
   * 배틀 상세 조회
   */
  @Transactional
  public BattleResponse.BattleDetail getBattleDetail(Long battleId, Long currentUserId) {
    Battle battle = getBattleByIdOrElseThrow(battleId);
    ExternalUserInfo userInfo = userCacheService.getUserInfo(battle.getCreatorId());

    battle.incrementViews();

    // 생성자는 모든 상태의 아이템 조회, 일반 사용자는 활성화된 아이템만 조회
    BattleItemStatus status = isCreator(battle, currentUserId)
        ? null
        : BattleItemStatus.ACTIVE;

    List<BattleItem> battleItems = battleItemRepository.findByBattleIdWithBattleVote(
        battleId, currentUserId, status);

    List<BattleItemInfo> items = battleItems.stream()
        .map(item -> BattleItemInfo.from(item, currentUserId))
        .toList();

    return BattleResponse.BattleDetail.from(battle, userInfo, items);
  }

  private boolean isCreator(Battle battle, Long userId) {
    return userId != null && userId.equals(battle.getCreatorId());
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
   * 제품 첨부파일(대표 이미지) 생성
   */
  private void createBattleAttachmentFiles(AttachmentInfo thumbnailAttachmentInfo, Battle battle) {
    BattleAttachmentFile primaryImage = createAttachmentFile(thumbnailAttachmentInfo, battle, 0, true);
    battleAttachmentFileRepository.save(primaryImage);
  }

  /**
   * 개별 첨부파일 생성 헬퍼 메서드
   */
  private BattleAttachmentFile createAttachmentFile(AttachmentInfo info, Battle battle, int order, boolean isPrimary) {
    return info.toEntity(battle, order, isPrimary);
  }

  private Battle getBattleByIdOrElseThrow(Long battleId) {
    return battleRepository.findById(battleId)
        .orElseThrow(() -> new RestApiException(BattleErrorCode.BATTLE_NOT_FOUND));
  }


}