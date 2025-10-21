package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.domain.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, Long> {

}
