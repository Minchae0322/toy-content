package com.example.toycontent.app.hashtag.repository;

import com.example.toycontent.app.hashtag.domain.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

}
