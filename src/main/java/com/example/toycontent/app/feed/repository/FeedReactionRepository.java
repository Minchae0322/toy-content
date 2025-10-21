package com.example.toycontent.app.feed.repository;

import com.example.toycontent.app.feed.domain.FeedReaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {

}
