package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.common.dto.CursorResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.FeedCursorResponse;
import com.example.toycontent.app.feed.domain.FeedReaction;
import com.example.toycontent.app.feed.repository.FeedHashtagRepository;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import com.example.toycontent.app.feed.controller.dto.FeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import com.example.toycontent.app.feed.domain.FeedHashtag;
import com.example.toycontent.app.feed.repository.FeedAttachmentFileRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import com.example.toycontent.app.hashtag.repository.HashtagRepository;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserRewardInfo;
import com.example.toycontent.app.reward.exp.service.ExpGrantService;
import com.example.toycontent.app.reward.exp.service.UserRewardService;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantInfo;
import com.example.toycontent.app.reward.exp.service.dto.ExpGrantResult;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.ExternalUserFollowingService;
import com.example.toycontent.external.user.service.ExternalUserInfoService;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedService {

  private final FeedRepository feedRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final HashtagRepository hashtagRepository;
  private final FeedAttachmentFileRepository feedAttachmentFileRepository;
  private final ExternalUserInfoService externalUserInfoService;
  private final ExternalUserFollowingService externalUserFollowingService;
  private final FeedReactionRepository feedReactionRepository;
  private final FeedHashtagRepository feedHashtagRepository;
  private final ExpGrantService expGrantService;
  private final UserRewardService userRewardService;

  private static final int HOT_FEED_RECENT_DAYS = 30;

  @Value("${feed.hot.min-views:5}")
  private int hotFeedMinViews;
  
  /**
   * 피드 목록 조회 (커서 페이징) - 탐색/검색용
   */
  public CursorResponse<FeedResponse.ListView> getFeedsWithCursor(Search condition, Long userId) {
    Integer requestSize = condition.getSize();
    condition.setSize(requestSize + 1);

    Optional.ofNullable(userId)
        .ifPresent(condition::setReaderId);

    List<Feed> feeds = feedRepository.findFeedsWithCursor(condition);

    Map<Long, List<FeedReaction>> userReactionsMap = getUserReactionsByFeedId(feeds, userId);

    List<FeedResponse.ListView> feedResponses = toListView(feeds, userReactionsMap);

    return CursorResponse.of(feedResponses, requestSize, FeedResponse.ListView::getFeedId);
  }

  private Map<Long, List<FeedReaction>> getUserReactionsByFeedId(List<Feed> feeds, Long userId) {
    return Optional.ofNullable(userId)
        .map(currentUserId -> {
          List<Long> feedsId = feeds.stream()
              .map(Feed::getId)
              .toList();

          return feedReactionRepository.findByFeedIdsAndUserId(feedsId, currentUserId)
              .stream()
              .collect(Collectors.groupingBy(reaction -> reaction.getFeed().getId()));

        }).orElse(Collections.emptyMap());
  }


  /**
   * 팔로우한 사용자의 피드 조회 (커서 페이징)
   */
  public FeedCursorResponse getFollowingFeeds(Following condition, Long userId) {
    Integer requestSize = condition.getSize();
    condition.setSize(requestSize + 1);
    condition.setReaderId(userId);

    List<Long> followingIds = externalUserFollowingService.getFollowingIds(userId);

    List<Feed> feeds = feedRepository.findFollowingFeeds(condition, followingIds);

    Map<Long, List<FeedReaction>> userReactionsMap = getUserReactionsByFeedId(feeds, userId);

    List<FeedResponse.ListView> feedResponses = toListView(feeds, userReactionsMap);

    return FeedCursorResponse.of(feedResponses, requestSize);
  }

  /**
   * Feed 리스트 -> ListView 변환 (공통 메서드).
   *
   * <p>썸네일/이미지 개수는 attachments를 한 번에 fetch해 feedId로 그룹화한 뒤
   * ListView.from에 명시적으로 전달한다. {@code feed.getAttachmentFiles()} LAZY 호출을
   * 피해 N+1을 차단하고, primary 보장 로직을 명시화.
   */
  private List<FeedResponse.ListView> toListView(List<Feed> feeds, Map<Long, List<FeedReaction>> userReactionsMap) {
    List<Long> creatorIds = feeds.stream()
        .map(Feed::getUserId)
        .toList();
    List<Long> feedIds = feeds.stream().map(Feed::getId).toList();

    Map<Long, ExternalUserInfo> externalUserInfoMap = externalUserInfoService.getUserInfos(
        creatorIds);

    Map<Long, UserRewardInfo> userRewardInfoMap = userRewardService.getUserRewardInfoMap(creatorIds);

    Map<Long, List<FeedAttachmentFile>> attachmentsByFeedId = feedRepository
        .findAttachmentsByFeedIds(feedIds)
        .stream()
        .collect(Collectors.groupingBy(a -> a.getFeed().getId()));

    return feeds.stream()
        .map(feed -> FeedResponse.ListView.from(
            feed,
            externalUserInfoMap.get(feed.getUserId()),
            userReactionsMap.get(feed.getId()),
            userRewardInfoMap.get(feed.getUserId()),
            attachmentsByFeedId.getOrDefault(feed.getId(), List.of())))
        .toList();
  }

  /**
   * 핫 피드 목록 조회 (실시간 계산)
   *
   * 최근 N일 이내 게시물만 대상으로 하여 성능 최적화
   */
  public Page<FeedResponse.HotFeedResponse> getHotFeeds(Pageable pageable) {
    // 실시간 핫 스코어 계산하여 조회
    return feedRepository.findAllByHotScore(HOT_FEED_RECENT_DAYS, hotFeedMinViews, pageable);
  }


  /**
   * 피드 단건 조회
   */
  public FeedResponse.Detail getFeed(Long feedId, Long userId) {
    Feed feed = findFeedById(feedId);

    List<FeedReaction> usersReactions = Optional.ofNullable(userId)
        .map(currentUserId -> feedReactionRepository.findByFeedIdAndUserIdAndIsActiveTrue(feedId, currentUserId))
        .orElse(Collections.emptyList());

    ExternalUserInfo userInfo = externalUserInfoService.getUserInfo(feed.getUserId());

    UserRewardInfo userRewardInfo = userRewardService.getUserRewardInfo(feed.getUserId());

    // 조회수 증가
    feed.incrementViewCount();

    return FeedResponse.Detail.from(feed, userInfo, usersReactions, userRewardInfo);
  }

  /**
   * 피드 생성
   */
  @Transactional
  public FeedResponse.FeedCreated createFeed(FeedRequest.CreateFeed request) {
    // 카테고리 조회 및 검증
    Category category = categoryRepository.findById(request.getSubCategoryId())
        .orElseThrow(() -> new RestApiException(FeedErrorCode.CATEGORY_NOT_FOUND));

    Product product = Optional.ofNullable(request.getProductId())
        .flatMap(productRepository::findById)
        .orElse(null);

    Feed feed = toEntity(request, category, product);

    Feed savedFeed = feedRepository.save(feed);

    // 피드 첨부파일 추가
    createFeedAttachmentFiles(
        request.getThumbnailAttachmentInfo(),
        request.getAttachmentFileInfos(),
        feed
    );

    // 새로운 해시태그 추가
    Optional.ofNullable(request.getHashtags())
        .orElse(Collections.emptyList())
        .stream()
        .map(this::findOrCreateHashtag)
        .map(hashtag -> FeedHashtag.create(feed, hashtag))
        .forEach(feed.getHashtags()::add);


    // EXP 지급: 피드 작성 + 완성도 보너스
    ExpGrantResult createGrant = expGrantService.grantFeedCreate(request.getUserId(), savedFeed.getId());

    ExpGrantResult qualityGrant = null;
    int qualityScore = savedFeed.calculateQualityScore();
    if (qualityScore >= 3 && !savedFeed.getQualityBonusGranted()) {
      qualityGrant = expGrantService.grantFeedQualityBonus(request.getUserId(), savedFeed.getId(), qualityScore);
      savedFeed.markQualityBonusGranted();
    }

    ExpGrantInfo expGrant = qualityGrant != null
        ? ExpGrantInfo.aggregate(createGrant, qualityGrant)
        : ExpGrantInfo.aggregate(createGrant);

    return FeedResponse.FeedCreated.of(savedFeed, expGrant);
  }

  private Feed toEntity(FeedRequest.CreateFeed request, Category category, Product product) {
    return Feed.builder()
            .userId(request.getUserId())
            .product(product)
            .productNameCustom(
                    request.getProductNameCustom() == null && product != null
                            ? product.getName()
                            : request.getProductNameCustom())
            .category(category)
            .review(request.getReview())
            .evaluation(request.getEvaluation())
            .buyPlace(request.getBuyPlace())
            .buyPrice(request.getBuyPrice())
            .price(request.getPrice())
            .build();
  }

  /**
   * 제품 첨부파일(대표 이미지 + 상세 이미지) 생성
   * - 썸네일(대표 이미지)와 상세 이미지 파일을 각각 엔티티로 변환 후 일괄 저장
   */
  private void createFeedAttachmentFiles(AttachmentInfo thumbnailAttachmentInfo,
      List<AttachmentInfo> attachmentInfos,
      Feed feed) {
    // 대표 이미지 파일 생성
    FeedAttachmentFile primaryImage = createAttachmentFile(thumbnailAttachmentInfo, feed, 0, true);

    // 상세 이미지 파일 생성 (순서 부여)
    List<FeedAttachmentFile> detailFiles = IntStream.range(0, attachmentInfos.size())
        .mapToObj(i -> createAttachmentFile(attachmentInfos.get(i), feed, i + 1, false))
        .toList();

    // 대표 + 상세 이미지 통합 저장
    feedAttachmentFileRepository.saveAll(
        Stream.concat(Stream.of(primaryImage), detailFiles.stream()).toList()
    );
  }

  /**
   * 개별 첨부파일 생성 헬퍼 메서드
   * - AttachmentInfo → ProductAttachmentFile 변환
   * - 순서(order)와 대표 여부(isPrimary) 설정 포함
   */
  private FeedAttachmentFile createAttachmentFile(AttachmentInfo info, Feed feed, int order, boolean isPrimary) {
    return info.toEntity(feed, order, isPrimary);
  }

  /**
   * 피드 수정 (첨부파일은 수정 불가)
   */
  @Transactional
  public FeedResponse.FeedCreated updateFeed(Long feedId, FeedRequest.UpdateFeed request, Long currentUserId) {
    Feed feed = findFeedById(feedId);

    // 권한 확인
    if (!currentUserId.equals(feed.getUserId())) {
      throw new RestApiException(FeedErrorCode.CREATOR_NOT_MATCH);
    }

    // 카테고리 조회
    Category category = Optional.ofNullable(request.getCategoryId())
        .map(id -> categoryRepository.findById(id)
            .orElseThrow(() -> new RestApiException(FeedErrorCode.CATEGORY_NOT_FOUND)))
        .orElse(feed.getCategory());

    // 상품 조회
    Product product = Optional.ofNullable(request.getProductId())
        .flatMap(productRepository::findById)
        .orElse(null);

    // 기본 정보 업데이트
    feed.update(request, category, product);

    // 해시태그 업데이트
    deleteAllFeedHashtagsByFeedId(feed);

    Optional.ofNullable(request.getHashtags())
        .orElse(Collections.emptyList())
        .stream()
        .map(this::findOrCreateHashtag)
        .map(hashtag -> FeedHashtag.create(feed, hashtag))
        .forEach(feed.getHashtags()::add);

    return FeedResponse.FeedCreated.of(feed);
  }

  private void deleteAllFeedHashtagsByFeedId(Feed feed) {
    feedHashtagRepository.deleteAllByFeed_Id(feed.getId());
    feed.getHashtags().clear();
  }

  /**
   * 피드 삭제
   */
  @Transactional
  public void deleteFeed(Long feedId, Long userId, boolean isAdmin) {
    Feed feed = findFeedById(feedId);

    if(!feed.getUserId().equals(userId) && !isAdmin) {
      throw new RestApiException(FeedErrorCode.CREATOR_NOT_MATCH);
    }

    feed.delete();

    //해시 태그 사용횟수 감소
    feed.getHashtags().forEach(
        feedHashtag -> feedHashtag.getHashtag().decrementUsageCount()
    );
  }

  /**
   * 해시태그 찾기 또는 생성
   */
  private Hashtag findOrCreateHashtag(String name) {
    String normalizedName = name.trim().toLowerCase();
    Hashtag hashtag = hashtagRepository.findByName(normalizedName)
        .orElseGet(() -> {
          Hashtag newHashtag = Hashtag.builder()
              .name(normalizedName)
              .usageCount(0L)
              .build();
          return hashtagRepository.save(newHashtag);
        });

    hashtag.incrementUsageCount();

    return hashtag;
  }

  /**
   * 피드 조회 (존재하지 않으면 예외 발생)
   */
  private Feed findFeedById(Long feedId) {
    return feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));
  }


}
