package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.common.dto.CursorResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.controller.dto.FeedCursor;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.FeedCursorResponse;
import com.example.toycontent.app.feed.domain.FeedReaction;
import com.example.toycontent.app.feed.event.FeedViewedEvent;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

  private final com.example.toycontent.app.product.service.ProductPopularityService productPopularityService;
  private final FeedRepository feedRepository;
  private final FeedQueryService feedQueryService;
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
  private final ApplicationEventPublisher eventPublisher;


  @Value("${feed.hot.min-views:5}")
  private int hotFeedMinViews;
  
  /**
   * 피드 목록 조회 (커서 페이징) - 탐색/검색용
   *
   * <p>커넥션 점유 경계 분리 (2026-08-30): DB 조회+매핑은 {@link FeedQueryService}의 좁은
   * readOnly 트랜잭션에서 끝내고, userInfo(Redis/auth-service)는 트랜잭션 밖에서 채운다.
   * 종전에는 클래스 레벨 트랜잭션이 외부 I/O 왕복 동안에도 커넥션을 잡고 있었다
   * (T2-B 극한에서 acquire 10.1s의 배경). NOT_SUPPORTED는 클래스 레벨 @Transactional을
   * 이 메서드에서만 무효화한다.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public CursorResponse<FeedResponse.ListView> getFeedsWithCursor(Search condition, Long userId) {
    Integer requestSize = condition.getSize() != null ? condition.getSize() : 20;
    condition.setSize(requestSize + 1);

    Optional.ofNullable(userId)
        .ifPresent(condition::setReaderId);

    FeedQueryService.ListViews loaded = feedQueryService.loadListViews(condition, userId);

    Map<Long, ExternalUserInfo> userInfoMap =
        externalUserInfoService.getUserInfos(loaded.creatorIds());

    List<FeedResponse.ListView> feedResponses = loaded.views();
    IntStream.range(0, feedResponses.size())
        .forEach(i -> feedResponses.get(i).setUserInfo(userInfoMap.get(loaded.creatorIds().get(i))));

    return CursorResponse.of(feedResponses, requestSize,
        v -> FeedCursor.encode(v.getCreatedAt(), v.getFeedId()));
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
   * 작성일 창 없음 — Reddit식 hot_score 자체가 시간 항을 품고 있어 새 글이 자연히 위로 온다.
   * 최소 조회수(feed.hot.min-views)만 거른다.
   */
  // 핫리스트 캐시 (2026-08-23): 사용자 무관. TTL 5분 + 전체 재계산 시 evict.
  // 캐시 히트 시 트랜잭션·커넥션도 안 탄다 (@Cacheable이 @Transactional보다 먼저).
  // 키의 'v2:' 접두사: 반환형이 Page → List로 바뀌어(count 쿼리 제거) 배포 직후 TTL 안에 남은
  // 구 Page 엔트리를 List로 캐스팅하다 깨지지 않도록 키 공간을 분리한다.
  @org.springframework.cache.annotation.Cacheable(
      cacheNames = com.example.toycontent.app.config.CacheConfig.HOT_FEEDS,
      key = "'v2:' + #pageable.toString()")
  public List<FeedResponse.HotFeedResponse> getHotFeeds(Pageable pageable) {
    return feedRepository.findAllByHotScore(hotFeedMinViews, pageable);
  }


  /**
   * 피드 단건 조회 — readOnly 트랜잭션에서 돈다.
   *
   * <p>조회수 증가는 {@link com.example.toycontent.app.feed.event.FeedViewedEvent} 발행으로
   * 대체됐다 (2026-08-23). 종전에는 컨트롤러가 별도 쓰기 트랜잭션을 먼저 호출해 요청마다
   * 커넥션을 두 번 획득했는데, 극한 부하(풀 12·acquire 9.1s)에서 detail이 풀 압력을 배로
   * 만드는 원인이었다. 이제 UPDATE는 커밋 후 리스너가 수행하고, 본인 조회 몫은 응답에서
   * +1 보정된다 (Detail.from).
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public FeedResponse.Detail getFeed(Long feedId, Long userId) {
    // 커넥션 점유 경계 분리 (2026-08-30): DB 조회+매핑(이벤트 발행 포함)은 좁은 트랜잭션,
    // userInfo(Redis/auth-service)는 트랜잭션 밖. getFeedsWithCursor와 같은 구조.
    FeedQueryService.DetailView loaded = feedQueryService.loadDetail(feedId, userId);

    loaded.detail().setUserInfo(externalUserInfoService.getUserInfo(loaded.creatorId()));

    return loaded.detail();
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
    if (product != null) {
      productPopularityService.refresh(product.getId());
    }

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
