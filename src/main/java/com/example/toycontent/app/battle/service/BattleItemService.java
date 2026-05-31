package com.example.toycontent.app.battle.service;

import static com.example.toycontent.app.common.utils.BattleItemRankingCalculator.setRanking;

import com.example.toycontent.app.battle.controller.dto.BattleItemCommentResponse.BattleItemCommentSummary;
import com.example.toycontent.app.battle.controller.dto.BattleRequest;
import com.example.toycontent.app.battle.controller.dto.BattleRequest.ItemRequest;
import com.example.toycontent.app.battle.controller.dto.BattleResponse.BattleItemInfo;
import com.example.toycontent.app.battle.controller.dto.BattleVoteRequest;
import com.example.toycontent.app.battle.audit.VoteAuditLogger;
import com.example.toycontent.app.battle.domain.Battle;
import com.example.toycontent.app.battle.domain.BattleItem;
import com.example.toycontent.app.battle.domain.BattleItemEventEntry;
import com.example.toycontent.app.battle.domain.BattleVote;
import com.example.toycontent.app.battle.repository.BattleItemCommentRepository;
import com.example.toycontent.app.battle.repository.BattleItemEventEntryRepository;
import com.example.toycontent.app.battle.repository.BattleItemRepository;
import com.example.toycontent.app.battle.repository.BattleRepository;
import com.example.toycontent.app.battle.repository.BattleVoteRepository;
import com.example.toycontent.app.common.voter.VoterId;
import com.example.toycontent.app.common.enumuration.BattleItemStatus;
import com.example.toycontent.app.common.enumuration.ItemAddPermissionType;
import com.example.toycontent.app.common.enumuration.VoteType;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.BattleErrorCode;
import com.example.toycontent.app.common.exception.impl.ProductErrorCode;
import com.example.toycontent.app.common.utils.YoutubeUtils;
import com.example.toycontent.app.notification.NotificationService;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantInfo;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantResult;
import com.example.toycontent.external.user.dto.ExternalAttachmentFileDto;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
  private final BattleItemCommentRepository battleItemCommentRepository;
  private final BattleItemEventEntryRepository battleItemEventEntryRepository;
  private final ProductRepository productRepository;
  private final ExpGrantService expGrantService;
  private final ExternalUserInfoService externalUserInfoService;
  private final NotificationService notificationService;
  private final VoteAuditLogger voteAuditLogger;

  private static final int MAX_ADDITIONAL_ITEMS = 3;
  private static final int AUTO_REVIEW_REPORT_COUNT = 3;

  /** 이벤트 ID 입력을 허용하는 배틀 화이트리스트. 이벤트 추가 시 한 줄씩 등록. */
  private static final Set<Long> EVENT_BATTLE_IDS = Set.of(21L);

  /**
   * 배틀 아이템 목록 조회
   * - 일반 사용자: ACTIVE 상태 아이템만 노출
   * - 배틀 생성자 / 어드민: condition.status 로 자유롭게 필터링 (null이면 전체)
   * - 각 아이템에 BEST 코멘트 요약과 조회자의 투표 정보를 함께 구성
   */
  public List<BattleItemInfo> getBattleItems(Long battleId, Long userId, boolean isAdmin,
      BattleRequest.BattleItemsSearchCondition condition) {

    Battle battle = getBattleById(battleId);
    BattleItemStatus statusFilter = resolveStatusFilter(battle, userId, isAdmin, condition);

    List<BattleItem> battleItems = battleItemRepository.findByBattleId(battleId, userId, statusFilter);

    List<Long> itemIds = battleItems.stream()
        .map(BattleItem::getId)
        .toList();

    // 현재 사용자의 투표 정보만 별도 조회
    Map<Long, BattleVote> userVoteMap = battleVoteRepository
        .findUserVotesByBattleItemIds(itemIds, userId);

    // 아이템별 BEST 코멘트 + 코멘트 수 일괄 조회
    Map<Long, BattleItemCommentSummary> commentSummaryMap = itemIds.isEmpty()
        ? Collections.emptyMap()
        : battleItemCommentRepository.findBestCommentsAndCountByItemIds(itemIds)
            .stream()
            .map(BattleItemCommentSummary::from)
            .collect(Collectors.toMap(
                BattleItemCommentSummary::getBattleItemId,
                summary -> summary
            ));

    return setRanking(
        battleItems.stream()
            .map(item -> BattleItemInfo.from(item,
                commentSummaryMap.get(item.getId()),
                userVoteMap.get(item.getId())))
            .collect(Collectors.toList())
    );
  }

  /**
   * 일반 사용자는 ACTIVE만, 생성자 또는 어드민은 요청한 상태(null이면 전체)로 필터링.
   */
  private BattleItemStatus resolveStatusFilter(Battle battle, Long userId, boolean isAdmin,
      BattleRequest.BattleItemsSearchCondition condition) {
    boolean isBattleCreator = userId != null && userId.equals(battle.getCreatorId());

    if (!isBattleCreator && !isAdmin) {
      return BattleItemStatus.ACTIVE;
    }

    return condition != null ? condition.getStatus() : null;
  }

  /**
   * 배틀 아이템 추가
   */
  @Transactional
  public ExpGrantInfo addBattleItems(Long battleId, Long userId, BattleRequest.AddBattleItems request) {
    Battle battle = getBattleById(battleId);
    List<ItemRequest> items = request.getItems();
    String eventId = normalizeEventId(request.getEventId());

    validateItemAddition(battle, userId, items, eventId);
    List<BattleItem> savedItems = addItemsByPermission(battle, userId, items);
    saveEventEntriesIfPresent(savedItems, eventId);
    notifyCreatorOnItemAddition(battle, userId, savedItems);

    ExpGrantResult grant = expGrantService.grantBattleItemAdd(userId, battleId);
    return ExpGrantInfo.aggregate(grant);
  }

  private void validateItemAddition(Battle battle, Long userId, List<ItemRequest> items,
      String eventId) {
    // 생성자 권한 확인
    if (battle.getItemAddPermissionType() == ItemAddPermissionType.CREATOR_ONLY) {
      validateBattleCreator(battle, userId);
    }

    // 최대 3개까지만 추가 가능
    if (items.size() > MAX_ADDITIONAL_ITEMS) {
      throw new RestApiException(BattleErrorCode.TOO_MANY_ITEMS);
    }

    // 이벤트 ID는 화이트리스트 배틀에서만 허용. 없으면 통과.
    if (eventId != null && !EVENT_BATTLE_IDS.contains(battle.getId())) {
      throw new RestApiException(BattleErrorCode.EVENT_ID_NOT_ALLOWED);
    }
  }

  /** 공백만 입력된 eventId는 미입력으로 취급해 entry 저장을 건너뛴다. */
  private static String normalizeEventId(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return raw.trim();
  }

  private void saveEventEntriesIfPresent(List<BattleItem> savedItems, String eventId) {
    if (eventId == null) {
      return;
    }
    List<BattleItemEventEntry> entries = savedItems.stream()
        .map(item -> BattleItemEventEntry.of(item.getId(), eventId))
        .toList();
    battleItemEventEntryRepository.saveAll(entries);
  }

  private List<BattleItem> addItemsByPermission(Battle battle, Long userId, List<ItemRequest> items) {
    ItemAddPermissionType permission = battle.getItemAddPermissionType();

    if (permission == ItemAddPermissionType.PUBLIC_APPROVAL) {
      return requestAddBattleItems(userId, battle, items);
    }
    return createBattleItems(userId, battle, items);
  }

  /**
   * 배틀 아이템 생성 (즉시 활성 상태로 등록)
   * - CREATOR_ONLY, PUBLIC 권한의 배틀에서 사용
   */
  @Transactional
  public List<BattleItem> createBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    return saveBattleItems(userId, battle, request, BattleItemStatus.ACTIVE);
  }

  /**
   * 배틀 아이템 승인 요청 (검토 대기 상태로 등록)
   * - PUBLIC_APPROVAL 권한의 배틀에서 사용
   * - 배틀 생성자가 승인해야 활성화됨
   */
  @Transactional
  public List<BattleItem> requestAddBattleItems(Long userId, Battle battle, List<ItemRequest> request) {
    return saveBattleItems(userId, battle, request, BattleItemStatus.UNDER_REVIEW);
  }

  private List<BattleItem> saveBattleItems(Long userId, Battle battle, List<ItemRequest> request,
      BattleItemStatus status) {
    List<BattleItem> battleItems = request.stream()
        .map(itemRequest -> createBattleItem(userId, battle, itemRequest, status))
        .toList();

    return battleItemRepository.saveAll(battleItems);
  }

  /**
   * 아이템 추가 시 배틀 생성자에게 알림.
   *
   * <p>본인이 자기 배틀에 추가한 경우는 외부 유저 정보 조회까지 모두 skip.
   * {@code PUBLIC_FREE}는 즉시 추가 알림, {@code PUBLIC_APPROVAL}은 승인 요청 알림.
   *
   * <p>한 요청에 여러 아이템이 들어오면 첫 아이템을 대표로 한 묶음 알림 1건으로
   * 발송 — 사용자에게 알림이 파바박 날아가지 않도록.
   */
  private void notifyCreatorOnItemAddition(Battle battle, Long actorId, List<BattleItem> savedItems) {
    if (battle.getCreatorId().equals(actorId)) {
      return;
    }

    ItemAddPermissionType permission = battle.getItemAddPermissionType();
    if (permission == ItemAddPermissionType.CREATOR_ONLY) {
      return;
    }

    BattleItem first = savedItems.stream().findFirst().orElse(null);
    if (first == null) {
      return;
    }
    int additionalCount = savedItems.size() - 1;

    ExternalUserInfo actor = externalUserInfoService.getUserInfo(actorId);
    String actorNickname = actor.getNickname();
    String actorProfileImageUrl = Optional.ofNullable(actor.getProfileImageFile())
        .map(ExternalAttachmentFileDto::getFileUrl)
        .orElse(null);

    if (permission == ItemAddPermissionType.PUBLIC_APPROVAL) {
      notificationService.notifyBattleItemApprovalRequest(
          battle.getCreatorId(), actorId, actorNickname, actorProfileImageUrl,
          battle.getId(), battle.getTitle(),
          first.getId(), first.getDisplayName(), additionalCount);
    } else {
      notificationService.notifyBattleItemAdded(
          battle.getCreatorId(), actorId, actorNickname, actorProfileImageUrl,
          battle.getId(), battle.getTitle(),
          first.getId(), first.getDisplayName(), additionalCount);
    }
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
   * - 단일 투표(SINGLE): 하나의 아이템에만 투표 가능, 재투표 시 기존 표를 갈아엎는다
   * - 복수 투표(MULTIPLE): 여러 아이템에 투표 가능, 재투표 시 기존 표를 갈아엎는다
   * - 게스트(VoterId.guest)도 투표 가능. EXP 적립은 로그인 사용자만 받는다.
   */
  @Transactional
  public ExpGrantInfo vote(Long battleId, VoterId voter, BattleVoteRequest.Vote request) {
    Battle battle = getBattleById(battleId);
    List<BattleVoteRequest.VoteItem> voteItems = request.getVotes();
    List<BattleVote> existingVotes = findVotesByVoter(battleId, voter);

    if (battle.isSingleVote()) {
      validateSingleVote(voteItems);
    } else {
      validateMultipleVote(voteItems);
    }
    removeExistingVotesIfPresent(battle, existingVotes);

    List<BattleVote> newVotes = createVotes(battle, voter, voteItems);
    battleVoteRepository.saveAll(newVotes);
    applyVoteStatistics(battle, newVotes);

    voteAuditLogger.logVote(battleId, voter, request);

    if (voter.isGuest()) {
      return ExpGrantInfo.aggregate();
    }
    ExpGrantResult grant = expGrantService.grantBattleVote(voter.userId(), battleId);
    return ExpGrantInfo.aggregate(grant);
  }

  private List<BattleVote> findVotesByVoter(Long battleId, VoterId voter) {
    if (voter.isUser()) {
      return battleVoteRepository.findByBattle_IdAndUserId(battleId, voter.userId());
    }
    return battleVoteRepository.findByBattle_IdAndGuestId(battleId, voter.guestId());
  }

  /**
   * 재투표 시 기존 투표를 통계에서 되돌리고 삭제한다.
   * Hibernate 기본 action queue가 insert를 delete보다 먼저 처리해
   * unique 제약(uk_battle_user_rank / uk_battle_guest_rank) 충돌이 발생하므로
   * 명시적으로 flush 하여 새 표 insert 전에 DELETE를 선행시킨다.
   */
  private void removeExistingVotesIfPresent(Battle battle, List<BattleVote> existingVotes) {
    if (existingVotes.isEmpty()) {
      return;
    }

    rollbackVoteStatistics(battle, existingVotes);
    battle.getVotes().removeAll(existingVotes);
    battleVoteRepository.deleteAll(existingVotes);
    battleVoteRepository.flush();
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
  private void validateSingleVote(List<BattleVoteRequest.VoteItem> voteItems) {
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
  public void cancelVote(Long battleId, VoterId voter) {
    Battle battle = getBattleById(battleId);

    List<BattleVote> votes = voter.isUser()
        ? battleVoteRepository.findByBattleAndUserId(battle, voter.userId())
        : battleVoteRepository.findByBattleAndGuestId(battle, voter.guestId());

    if (votes.isEmpty()) {
      throw new RestApiException(BattleErrorCode.VOTE_NOT_FOUND);
    }

    rollbackVoteStatistics(battle, votes);

    battle.getVotes().removeAll(votes);
    battleVoteRepository.deleteAll(votes);

    voteAuditLogger.logCancelVote(battleId, voter);
  }


  private List<BattleVote> createVotes(Battle battle, VoterId voter,
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
              .userId(voter.userId())
              .guestId(voter.guestId())
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