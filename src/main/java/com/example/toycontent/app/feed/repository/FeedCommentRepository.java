package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.controller.dto.FeedCommentResponse.CommentItem;
import com.example.toycontent.app.feed.domain.FeedComment;
import com.example.toycontent.app.feed.repository.querydsl.FeedCommentRepositoryCustom;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long>,
    FeedCommentRepositoryCustom {


  Optional<FeedComment> findByIdAndFeedId(Long commentId, Long feedId);


}
