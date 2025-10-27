package com.example.toycontent.app.hashtag.service;

import com.example.toycontent.app.hashtag.controller.dto.HashtagResponse.HotHashtagResponse;
import com.example.toycontent.app.hashtag.controller.dto.HashtagSearchCondition;
import com.example.toycontent.app.hashtag.repository.HashtagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class HashtagService {

  private final HashtagRepository hashtagRepository;

  /**
   * 인기 해시태그 조회 (사용 횟수 많은 순)
   */
  public Page<HotHashtagResponse> getHotHashtags(HashtagSearchCondition condition, Pageable pageable) {
    return hashtagRepository.findHotHashtags(condition, pageable)
        .map(HotHashtagResponse::from);
  }
}
