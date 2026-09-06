package com.example.toycontent.app.common.enumuration;

/**
 * 알림 타입별 전달 보장 등급.
 *
 * <p>등급은 "유실이 허용되는가"가 아니라 다음 세 질문으로 정한다. 하나라도 "예"면 {@link #GUARANTEED}.
 * <ol>
 *   <li>시효가 있나 - 특정 시각을 놓치면 알림의 가치가 사라지는가</li>
 *   <li>대체 경로가 없나 - 알림이 안 와도 사용자가 원본 화면에서 스스로 알아차릴 수 있는가</li>
 *   <li>타인의 흐름을 막나 - 수신자가 모르면 다른 사용자의 작업이 대기 상태로 남는가</li>
 * </ol>
 *
 * <p>{@link #BEST_EFFORT}에 outbox를 쓰지 않는 이유는 비용이 아니다. 보장을 사면 at-least-once(중복)와
 * 릴레이 지연이 확정적으로 따라오는데, 원본이 화면에 남아 있는 알림에서는 그 둘이 순손실이기 때문이다.
 * 판단 과정은 docs/notifications/알림개선.md.
 */
public enum DeliveryGuarantee {

  /** 커밋 직후 즉시 전송. 실패하면 로그만 남기고 유실을 허용한다 (기존 AFTER_COMMIT 경로). */
  BEST_EFFORT,

  /**
   * 같은 트랜잭션에 outbox 행을 남기고 커밋 직후 즉시 전송한다. ack가 오면 SENT로 표시하고,
   * 남은 PENDING은 ShedLock 리더 릴레이가 주기적으로 다시 보낸다. 우리 서비스 안에서 유실되지 않는다.
   * 브로커 내구성(복제 수)과 소비자 멱등은 이 등급이 보장하지 않는다.
   */
  GUARANTEED
}
