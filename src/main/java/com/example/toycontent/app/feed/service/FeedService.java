package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.Product.repository.ProductRepository;
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
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import com.example.toycontent.app.hashtag.repository.HashtagRepository;
import jakarta.transaction.Transactional;
import java.util.List;
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

  /**
   * 피드 목록 조회 (페이징)
   */
  public Page<FeedResponse.ListView> getFeeds(Pageable pageable, FeedSearchCondition condition) {
    List<Feed> feeds = feedRepository.findFeedsWithSearchCondition(pageable, condition);
    Long totalCount = feedRepository.countFeedsWithSearchCondition(condition);

    return new PageImpl<>(
        feeds.stream()
            .map(FeedResponse.ListView::from)
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
        .map(FeedResponse.ListView::from)
        .toList();

    return FeedResponse.FeedCursorResponse.of(feedResponses, requestSize);
  }

  /**
   * 피드 전체 목록 조회
   */
  public List<FeedResponse.ListView> getFeedList(FeedSearchCondition condition) {
    List<Feed> feeds = feedRepository.findFeedsWithSearchCondition(condition);

    return feeds.stream()
        .map(FeedResponse.ListView::from)
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

    // Feed 엔티티 생성
    Feed feed = Feed.builder()
        .userId(request.getUserId())
        .productNameCustom(request.getProductNameCustom())
        .category(category)
        .review(request.getReview())
        .buyPrice(request.getBuyPrice())
        .price(request.getPrice())
        .viewCount(0)
        .build();

    // 상품 ID가 있는 경우 상품 연결
    if (request.getProductId() != null) {
      Product product = productRepository.findById(request.getProductId())
          .orElseThrow(() -> new RestApiException(FeedErrorCode.PRODUCT_NOT_FOUND));
      feed.setProduct(product);
    }

    // 이미지 URL 처리
    if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
      for (int i = 0; i < request.getImageUrls().size(); i++) {
        FeedAttachmentFile attachmentFile = FeedAttachmentFile.builder()
            .feed(feed)
            .fileUrl(request.getImageUrls().get(i))
            .displayOrder(i)
            .build();
        feed.getAttachmentFiles().add(attachmentFile);
      }
    }

    // 해시태그 처리
    if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
      for (String hashtagName : request.getHashtags()) {
        Hashtag hashtag = findOrCreateHashtag(hashtagName);
        FeedHashtag feedHashtag = FeedHashtag.create(feed, hashtag);
        feed.getHashtags().add(feedHashtag);
      }
    }

    Feed savedFeed = feedRepository.save(feed);
    return FeedResponse.Detail.from(savedFeed);
  }

  /**
   * 피드 수정
   */
  @Transactional
  public FeedResponse.Detail updateFeed(Long feedId, FeedRequest.UpdateFeed request) {
    Feed feed = findFeedById(feedId);

    // 권한 확인
    if (!feed.getUserId().equals(request.getUserId())) {
      throw new RestApiException(FeedErrorCode.UNAUTHORIZED_ACCESS);
    }

    // 카테고리 업데이트
    if (request.getCategoryId() != null) {
      Category category = categoryRepository.findById(request.getCategoryId())
          .orElseThrow(() -> new RestApiException(FeedErrorCode.CATEGORY_NOT_FOUND));
      feed.setCategory(category);
    }

    // 상품 업데이트
    if (request.getProductId() != null) {
      Product product = productRepository.findById(request.getProductId())
          .orElseThrow(() -> new RestApiException(FeedErrorCode.PRODUCT_NOT_FOUND));
      feed.setProduct(product);
    }

    // 기본 정보 업데이트
    feed.setProductNameCustom(request.getProductNameCustom());
    feed.setReview(request.getReview());
    feed.setBuyPrice(request.getBuyPrice());
    feed.setPrice(request.getPrice());

    // 기존 첨부파일 제거
    feed.getAttachmentFiles().clear();

    // 새로운 이미지 URL 추가
    if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
      for (int i = 0; i < request.getImageUrls().size(); i++) {
        FeedAttachmentFile attachmentFile = FeedAttachmentFile.builder()
            .feed(feed)
            .fileUrl(request.getImageUrls().get(i))
            .displayOrder(i)
            .build();
        feed.getAttachmentFiles().add(attachmentFile);
      }
    }

    // 기존 해시태그 제거
    feed.getHashtags().clear();

    // 새로운 해시태그 추가
    if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
      for (String hashtagName : request.getHashtags()) {
        Hashtag hashtag = findOrCreateHashtag(hashtagName);
        FeedHashtag feedHashtag = FeedHashtag.create(feed, hashtag);
        feed.getHashtags().add(feedHashtag);
      }
    }

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

    return hashtagRepository.findByName(normalizedName)
        .orElseGet(() -> {
          Hashtag newHashtag = Hashtag.builder()
              .name(normalizedName)
              .usageCount(0)
              .build();
          return hashtagRepository.save(newHashtag);
        });
  }

  /**
   * 피드 조회 (존재하지 않으면 예외 발생)
   */
  private Feed findFeedById(Long feedId) {
    return feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));
  }
}
