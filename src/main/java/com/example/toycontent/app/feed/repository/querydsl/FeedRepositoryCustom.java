package com.example.toycontent.app.feed.repository.querydsl;

import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.HotFeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepositoryCustom {

  List<Feed> findFeedsWithCursor(Search condition);

  /**
   * 핫 스코어 기반 피드 조회
   *
   * @param recentDays  포함 대상 기간(일)
   * @param minViews    노출 자격 최소 조회수 (0이면 비활성)
   */
  Page<HotFeedResponse> findAllByHotScore(int minViews, Pageable pageable);

  List<Feed> findFollowingFeeds(Following condition, List<Long> followings);

  List<Feed> findByProductIdAndIsDeletedNot(Long productId, Boolean isDeleted, Long cursor,
      Integer size);

  /**
   * feedIds에 속한 모든 attachment를 한 번에 조회. service에서 그룹화하여
   * primary 추출(썸네일) + imageCount 산출에 사용된다.
   *
   * <p>feed.attachmentFiles 컬렉션 LAZY 초기화로 발생하던 N+1을 차단하기 위해 도입.
   */
  List<FeedAttachmentFile> findAttachmentsByFeedIds(List<Long> feedIds);

}
