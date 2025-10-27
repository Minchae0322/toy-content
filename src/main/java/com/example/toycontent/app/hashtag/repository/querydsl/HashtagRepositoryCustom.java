package com.example.toycontent.app.hashtag.repository.querydsl;

import com.example.toycontent.app.hashtag.controller.dto.HashtagSearchCondition;
import com.example.toycontent.app.hashtag.domain.Hashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HashtagRepositoryCustom {
  Page<Hashtag> findHotHashtags(HashtagSearchCondition condition, Pageable pageable);

}
