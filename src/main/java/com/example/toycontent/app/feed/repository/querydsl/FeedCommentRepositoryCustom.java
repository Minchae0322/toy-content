package com.example.toycontent.app.feed.repository.querydsl;

import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedCommentRepositoryCustom {
  Page<CommentItem> findVisibleCommentsWithReplies(Long feedId, Pageable pageable);
}
