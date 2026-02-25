package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.domain.FeedHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedHashtagRepository extends JpaRepository<FeedHashtag, Long> {

  void deleteAllByFeed_Id(Long feedId);
}
