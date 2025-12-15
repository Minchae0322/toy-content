package com.example.toycontent.app.feed.service;


import com.example.toycontent.app.common.exception.RestApiException;
import com.example.toycontent.app.common.exception.impl.FeedErrorCode;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest.CommentCreate;
import com.example.toycontent.app.feed.controller.dto.FeedCommentRequest.CommentUpdate;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse;
import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import com.example.toycontent.app.feed.domain.Feed;
import com.example.toycontent.app.feed.domain.FeedComment;
import com.example.toycontent.app.feed.repository.FeedCommentRepository;
import com.example.toycontent.app.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedCommentService {

  private final FeedRepository feedRepository;
  private final FeedCommentRepository feedCommentRepository;


  @Transactional
  public Page<CommentItem> getComments(Long feedId, Pageable pageable) {
    return feedCommentRepository.findByFeedIdAndDeletedFalse(feedId, pageable);
  }

  @Transactional
  public FeedCommentResponse.Created createComment(Long feedId, CommentCreate request, Long creatorId) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_NOT_FOUND));

    FeedComment comment = toFeedComment(feed, request, creatorId);

    feedCommentRepository.save(comment);
    feed.incrementCommentCount();

    return FeedCommentResponse.Created.of(comment);
  }

  private FeedComment toFeedComment(Feed feed, CommentCreate create, Long creatorId) {
    return FeedComment.builder()
        .content(create.getContent())
        .creatorId(creatorId)
        .feed(feed)
        .deleted(false)
        .build();
  }

  @Transactional
  public FeedCommentResponse.Updated updateComment(Long feedId, Long commentId, CommentUpdate request) {
    FeedComment comment = findFeedCommentByIdAndFeedIdOrElseThrow(commentId, feedId);

    comment.updateContent(request.getContent());

    return FeedCommentResponse.Updated.of(comment);
  }

  @Transactional
  public void deleteComment(Long feedId, Long commentId) {
    FeedComment comment = findFeedCommentByIdAndFeedIdOrElseThrow(commentId, feedId);

    comment.getFeed().decrementCommentCount();

    comment.delete();
  }

  private FeedComment findFeedCommentByIdAndFeedIdOrElseThrow(Long commentId, Long feedId) {
    return feedCommentRepository.findByIdAndFeedId(commentId, feedId)
        .orElseThrow(() -> new RestApiException(FeedErrorCode.FEED_COMMENT_NOT_FOUND));
  }


}

