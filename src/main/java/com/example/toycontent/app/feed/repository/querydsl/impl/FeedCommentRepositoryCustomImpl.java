package com.example.toycontent.app.feed.repository.querydsl.impl;

import static com.example.toycontent.app.feed.domain.QFeedComment.feedComment;

import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.ReplyItem;
import com.example.toycontent.app.feed.domain.FeedComment;
import com.example.toycontent.app.feed.domain.QFeedComment;
import com.example.toycontent.app.feed.repository.querydsl.FeedCommentRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentRepositoryCustomImpl implements FeedCommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<CommentItem> findVisibleCommentsWithReplies(Long feedId, Pageable pageable) {
    BooleanExpression visibleRootCondition = rootCommentOfFeed(feedId)
        .and(notDeletedOrHasActiveReply());

    List<FeedComment> roots = queryFactory
        .selectFrom(feedComment)
        .where(visibleRootCondition)
        .orderBy(feedComment.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    Long total = queryFactory
        .select(feedComment.count())
        .from(feedComment)
        .where(visibleRootCondition.or(activeReplyOfFeed(feedId)))
        .fetchOne();

    Map<Long, List<ReplyItem>> repliesByParent = fetchRepliesGroupedByParent(roots);

    List<CommentItem> items = roots.stream()
        .map(root -> {
          List<ReplyItem> replies = repliesByParent.getOrDefault(root.getId(), Collections.emptyList());

          return Boolean.TRUE.equals(root.getDeleted())
              ? CommentItem.deletedWithReplies(root, replies)
              : CommentItem.active(root, replies);
        })
        .collect(Collectors.toList());

    return new PageImpl<>(items, pageable, total != null ? total : 0L);
  }

  @Override
  public List<Long> findReplyCreatorIdsByParentId(Long parentCommentId) {
    return queryFactory
        .select(feedComment.creatorId).distinct()
        .from(feedComment)
        .where(
            feedComment.parent.id.eq(parentCommentId),
            feedComment.deleted.eq(false)
        )
        .fetch();
  }

  private Map<Long, List<ReplyItem>> fetchRepliesGroupedByParent(List<FeedComment> roots) {
    if (roots.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Long> rootIds = roots.stream()
        .map(FeedComment::getId)
        .toList();

    List<FeedComment> replies = queryFactory
        .selectFrom(feedComment)
        .where(
            feedComment.parent.id.in(rootIds),
            feedComment.deleted.eq(false)
        )
        .orderBy(feedComment.id.asc())
        .fetch();

    return replies.stream()
        .collect(Collectors.groupingBy(
            reply -> reply.getParent().getId(),
            Collectors.mapping(ReplyItem::from, Collectors.toList())
        ));
  }

  private BooleanExpression rootCommentOfFeed(Long feedId) {
    return feedComment.feed.id.eq(feedId)
        .and(feedComment.parent.isNull());
  }

  private BooleanExpression activeReplyOfFeed(Long feedId) {
    return feedComment.feed.id.eq(feedId)
        .and(feedComment.parent.isNotNull())
        .and(feedComment.deleted.eq(false));
  }

  private BooleanExpression notDeletedOrHasActiveReply() {
    QFeedComment reply = new QFeedComment("reply");
    return feedComment.deleted.eq(false)
        .or(JPAExpressions.selectOne()
            .from(reply)
            .where(
                reply.parent.id.eq(feedComment.id),
                reply.deleted.eq(false)
            )
            .exists());
  }
}
