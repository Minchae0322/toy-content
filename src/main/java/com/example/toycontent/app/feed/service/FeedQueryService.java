package com.example.toycontent.app.feed.service;

import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.controller.dto.FeedResponse;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import com.example.toycontent.app.feed.domain.FeedReaction;
import com.example.toycontent.app.feed.event.FeedViewedEvent;
import com.example.toycontent.app.feed.repository.FeedReactionRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.reward.controller.dto.RewardResponse.UserRewardInfo;
import com.example.toycontent.app.reward.exp.service.UserRewardService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 조회 핫패스(scroll·detail) 전용 DB 로더 — 트랜잭션(=커넥션 점유) 경계를
 * "DB 조회 + LAZY 초기화(DTO 매핑)"까지로 좁힌다.
 *
 * <p>userInfo(Redis/auth-service 호출)는 여기서 채우지 않는다. DTO의 userInfo 필드는
 * 값을 저장만 하므로 null로 매핑해 두고, 호출자(FeedService)가 트랜잭션 밖에서
 * setUserInfo로 채운다. 외부 I/O 왕복 동안 커넥션이 풀에 반납되는 것이 목적이다
 * (풀 12는 RDS max_connections 60에 묶인 값이라 점유 시간이 곧 수용량이다).
 *
 * <p>매핑을 트랜잭션 안에 두는 이유: ListView/Detail.from이 product·category·hashtags
 * LAZY 연관을 읽는다. 밖으로 빼면 OSIV가 커넥션을 몰래 재획득해 경계를 좁힌 의미가
 * 사라진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedQueryService {

  private final FeedRepository feedRepository;
  private final FeedReactionRepository feedReactionRepository;
  private final UserRewardService userRewardService;
  private final ApplicationEventPublisher eventPublisher;

  /** views와 creatorIds는 같은 인덱스가 같은 피드를 가리킨다 (userInfo 후주입용). */
  public record ListViews(List<FeedResponse.ListView> views, List<Long> creatorIds) {}

  public record DetailView(FeedResponse.Detail detail, Long creatorId) {}

  public ListViews loadListViews(Search condition, Long userId) {
    List<Feed> feeds = feedRepository.findFeedsWithCursor(condition);

    Map<Long, List<FeedReaction>> userReactionsMap = Optional.ofNullable(userId)
        .map(currentUserId -> {
          List<Long> feedsId = feeds.stream().map(Feed::getId).toList();
          return feedReactionRepository.findByFeedIdsAndUserId(feedsId, currentUserId)
              .stream()
              .collect(Collectors.groupingBy(reaction -> reaction.getFeed().getId()));
        }).orElse(Collections.emptyMap());

    List<Long> creatorIds = feeds.stream().map(Feed::getUserId).toList();
    List<Long> feedIds = feeds.stream().map(Feed::getId).toList();

    Map<Long, UserRewardInfo> userRewardInfoMap = userRewardService.getUserRewardInfoMap(creatorIds);

    Map<Long, List<FeedAttachmentFile>> attachmentsByFeedId = feedRepository
        .findAttachmentsByFeedIds(feedIds)
        .stream()
        .collect(Collectors.groupingBy(a -> a.getFeed().getId()));

    List<FeedResponse.ListView> views = feeds.stream()
        .map(feed -> FeedResponse.ListView.from(
            feed,
            null,
            userReactionsMap.get(feed.getId()),
            userRewardInfoMap.get(feed.getUserId()),
            attachmentsByFeedId.getOrDefault(feed.getId(), List.of())))
        .toList();

    return new ListViews(views, creatorIds);
  }

  public DetailView loadDetail(Long feedId, Long userId) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

    List<FeedReaction> usersReactions = Optional.ofNullable(userId)
        .map(currentUserId ->
            feedReactionRepository.findByFeedIdAndUserIdAndIsActiveTrue(feedId, currentUserId))
        .orElse(Collections.emptyList());

    UserRewardInfo userRewardInfo = userRewardService.getUserRewardInfo(feed.getUserId());

    eventPublisher.publishEvent(new FeedViewedEvent(feedId));

    return new DetailView(
        FeedResponse.Detail.from(feed, null, usersReactions, userRewardInfo),
        feed.getUserId());
  }
}
