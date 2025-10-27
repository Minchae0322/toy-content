package com.example.toycontent.app.hashtag.repository;

import com.example.toycontent.app.hashtag.domain.Hashtag;
import com.example.toycontent.app.hashtag.repository.querydsl.HashtagRepositoryCustom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long>, HashtagRepositoryCustom {

  Optional<Hashtag> findByName(String normalizedName);
}
