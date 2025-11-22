package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.domain.FeedComment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

  Optional<FeedComment> findByIdAndFeedId(Long commentId, Long feedId);
}
