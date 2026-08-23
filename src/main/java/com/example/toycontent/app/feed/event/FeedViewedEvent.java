package com.example.toycontent.app.feed.event;

/**
 * 피드 열람을 알리는 인-프로세스 이벤트.
 *
 * <p>조회 트랜잭션(readOnly)은 이 이벤트만 publish 하고, 조회수 UPDATE는
 * {@link FeedViewCountEventListener}가 커밋 이후(AFTER_COMMIT) 별도 스레드·별도
 * 트랜잭션에서 수행한다. 요청 경로에서 쓰기 트랜잭션과 두 번째 커넥션 획득을 제거하기 위함이다.
 *
 * <p>응답의 조회수는 이 UPDATE가 반영되기 전의 SELECT 값이므로, 본인 조회 몫은
 * 응답 DTO에서 +1 보정한다 ({@code FeedResponse.Detail.from}).
 */
public record FeedViewedEvent(Long feedId) {
}
