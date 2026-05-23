package com.example.toycontent.app.notification.hotcontent;

import java.util.List;

/**
 * 핫 콘텐츠 발견 전략. 새 콘텐츠 종류를 추가하려면 본 인터페이스 구현체를 추가하고
 * {@link HotContentType}에 항목을 더하면 된다.
 */
public interface HotContentSource {

  HotContentType type();

  List<HotContentCandidate> findTopCandidates(int limit);
}
