package com.example.toycontent.app.feed.repository.querydsl.impl;

import static com.example.toycontent.app.feed.domain.QFeedComment.feedComment;

import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import com.example.toycontent.app.feed.domain.QFeedComment;
import com.example.toycontent.app.feed.repository.querydsl.FeedCommentRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
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
  public Page<CommentItem> findByFeedIdAndDeletedFalse(Long feedId, Pageable pageable) {
    List<CommentItem> commentItems = queryFactory
        .select(Projections.fields(CommentItem.class,
            feedComment.id.as("commentId"),
            feedComment.creatorId,
            feedComment.creatorNickname,
            feedComment.creatorProfileUrl,
            feedComment.content,
            feedComment.createdAt,
            feedComment.updatedAt
        ))
        .from(feedComment)
        .where(
            feedComment.feed.id.eq(feedId),
            feedComment.deleted.eq(false)
        )
        .orderBy(feedComment.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    Long total = queryFactory
        .select(feedComment.count())
        .from(feedComment)
        .where(
            feedComment.feed.id.eq(feedId),
            feedComment.deleted.eq(false)
        )
        .fetchOne();

    return new PageImpl<>(commentItems, pageable, total != null ? total : 0L);
  }
}
