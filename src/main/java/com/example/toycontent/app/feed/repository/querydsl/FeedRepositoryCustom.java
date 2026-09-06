package com.example.toycontent.app.feed.repository.querydsl;

import com.example.toycontent.app.feed.controller.dto.FeedCondition.Following;
import com.example.toycontent.app.feed.controller.dto.FeedResponse.HotFeedResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCondition.Search;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedRepositoryCustom {

  List<Feed> findFeedsWithCursor(Search condition);

  /**
   * 핫 스코어 기반 피드 조회 — 상위 N건만 돌려주고 count 쿼리는 내지 않는다.
   * 클라이언트가 첫 페이지 상위 N건만 쓰고 totalElements를 읽지 않아 Page를 만들 이유가 없었다.
   *
   * @param minViews    노출 자격 최소 조회수 (0이면 비활성)
   * @param pageable    정렬·건수(offset 포함)
   */
  List<HotFeedResponse> findAllByHotScore(int minViews, Pageable pageable);

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
