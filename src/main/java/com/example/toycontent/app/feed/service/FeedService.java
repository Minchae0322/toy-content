package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.product.domain.Product;
import com.example.toycontent.app.product.repository.ProductRepository;
import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.category.repository.CategoryRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedRequest;
import com.example.toycontent.app.feed.controller.dto.FeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedSearchCondition;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import com.example.toycontent.app.feed.domain.FeedHashtag;
import com.example.toycontent.app.feed.repository.FeedAttachmentFileRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.file.domain.dto.AttachmentFileRequest.AttachmentInfo;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import com.example.toycontent.app.hashtag.repository.HashtagRepository;
import com.example.toycontent.external.user.dto.ExternalUserInfo;
import com.example.toycontent.external.user.service.UserCacheService;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
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
  private final UserCacheService userCacheService;


  /**
   * 피드 목록 조회 (페이징)
   */
  public Page<FeedResponse.ListView> getFeeds(Pageable pageable, FeedSearchCondition condition) {
    List<Feed> feeds = feedRepository.findFeedsWithSearchCondition(pageable, condition);
    Long totalCount = feedRepository.countFeedsWithSearchCondition(condition);

    return new PageImpl<>(
        feeds.stream()
            .map(feed ->  {

              ExternalUserInfo userInfo = userCacheService.getUserInfo(feed.getUserId());
              return FeedResponse.ListView.from(feed, userInfo);

            })
            .toList(),
        pageable, totalCount);
  }

  /**
   * 피드 목록 조회 (커서 페이징) - 인피니티 스크롤용
   */
  public FeedResponse.FeedCursorResponse getFeedsWithCursor(FeedSearchCondition condition) {

    // size+1개를 조회해서 다음 페이지 존재 여부 확인
    Integer requestSize = condition.getSize();
    condition.setSize(requestSize + 1);

    List<Feed> feeds = feedRepository.findFeedsWithCursor(condition);

    List<FeedResponse.ListView> feedResponses = feeds.stream()
        .map(feed -> {

          ExternalUserInfo userInfo = userCacheService.getUserInfo(feed.getUserId());
          return FeedResponse.ListView.from(feed, userInfo);

        })
        .toList();

    return FeedResponse.FeedCursorResponse.of(feedResponses, requestSize);
  }

  /**
   * 피드 전체 목록 조회
   */
  public List<FeedResponse.ListView> getFeedList(FeedSearchCondition condition) {
    List<Feed> feeds = feedRepository.findFeedsWithSearchCondition(condition);

    return feeds.stream()
        .map(feed -> {

          ExternalUserInfo userInfo = userCacheService.getUserInfo(feed.getUserId());
          return FeedResponse.ListView.from(feed, userInfo);

        })
        .toList();
  }

  /**
   * 피드 단건 조회
   */
  public FeedResponse.Detail getFeed(Long feedId) {
    Feed feed = findFeedById(feedId);

    // 조회수 증가
    feed.incrementViewCount();

    return FeedResponse.Detail.from(feed);
  }

  /**
   * 피드 생성
   */
  @Transactional
  public FeedResponse.Detail createFeed(FeedRequest.CreateFeed request) {
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


    return FeedResponse.Detail.from(savedFeed);
  }

  private Feed toEntity(FeedRequest.CreateFeed request, Category category, Product product) {
    return Feed.builder()
        .userId(request.getUserId())
        .productNameCustom(request.getProductNameCustom())
        .category(category)
        .review(request.getReview())
        .product(product)
        .buyPrice(request.getBuyPrice())
        .price(request.getPrice())
        .buyPlace(request.getBuyPlace())
        .viewCount(0)
        .evaluation(request.getEvaluation())
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
  public FeedResponse.Detail updateFeed(Long feedId, FeedRequest.UpdateFeed request, Long currentUserId) {
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
    List<FeedHashtag> newHashtags = Optional.ofNullable(request.getHashtags())
        .orElse(Collections.emptyList())
        .stream()
        .map(this::findOrCreateHashtag)
        .map(hashtag -> FeedHashtag.create(feed, hashtag))
        .toList();
    feed.updateHashtags(newHashtags);

    return FeedResponse.Detail.from(feed);
  }

  /**
   * 피드 삭제
   */
  @Transactional
  public void deleteFeed(Long feedId) {
    Feed feed = findFeedById(feedId);

    // 연관된 데이터들은 cascade 설정으로 자동 삭제됨
    feedRepository.delete(feed);
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
