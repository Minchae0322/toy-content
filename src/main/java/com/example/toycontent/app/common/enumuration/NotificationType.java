package com.example.toycontent.app.common.enumuration;

import static com.example.toycontent.app.common.enumuration.DeliveryGuarantee.BEST_EFFORT;
import static com.example.toycontent.app.common.enumuration.DeliveryGuarantee.GUARANTEED;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 타입. 세 번째 인자가 전달 보장 등급이며 분류 근거는 {@link DeliveryGuarantee}.
 *
 * <p>BEST_EFFORT: 원본이 화면에 남아 사용자가 스스로 발견할 수 있는 알림 (댓글·좋아요·팔로우·아이템 추가).
 * <br>GUARANTEED: 시효가 있거나(배틀 결과·D-7), 대체 경로가 없거나(초대·시스템),
 * 타인의 흐름을 막는(승인 요청) 알림.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

  // 댓글 - 댓글은 피드·아이템 화면에 남아 있다
  FEED_COMMENT("새 댓글", "%s님이 [%s] 피드에 댓글을 남겼습니다.", BEST_EFFORT),
  BATTLE_ITEM_COMMENT("새 댓글", "%s님이 [%s] 배틀의 [%s] 아이템에 댓글을 남겼습니다.", BEST_EFFORT),

  // 배틀 아이템 추가 - 승인 불필요 배틀은 목록에 바로 노출 / 승인 요청은 생성자가 모르면 요청자가 대기
  BATTLE_ITEM_ADDED("새 아이템", "%s님이 [%s] 배틀에 [%s] 아이템을 추가했어요.", BEST_EFFORT),
  BATTLE_ITEM_ADDED_BATCH("새 아이템", "%s님이 [%s] 배틀에 [%s] 외 %s개 아이템을 추가했어요.", BEST_EFFORT),
  BATTLE_ITEM_APPROVAL_REQUEST("승인 요청", "%s님이 [%s] 배틀에 [%s] 아이템 등록을 요청했어요. 승인해주세요.", GUARANTEED),
  BATTLE_ITEM_APPROVAL_REQUEST_BATCH("승인 요청", "%s님이 [%s] 배틀에 [%s] 외 %s개 아이템 등록을 요청했어요. 승인해주세요.", GUARANTEED),

  // 좋아요 - 좋아요 수에 반영된다. 최다 발생 타입이고 중복 알림이 유실보다 거슬린다
  FEED_LIKE("좋아요", "%s님이 [%s] 피드에 좋아요를 눌렀습니다.", BEST_EFFORT),
  BATTLE_ITEM_LIKE("좋아요", "%s님이 [%s] 배틀의 [%s] 아이템에 좋아요를 눌렀습니다.", BEST_EFFORT),

  // 소셜 - 팔로워 목록에 보인다
  FOLLOW("새 팔로우", "%s님이 회원님을 팔로우했습니다.", BEST_EFFORT),

  // 배틀 - 초대는 알 경로가 없고, 결과·D-7은 그 시각에만 의미가 있다
  BATTLE_INVITE("배틀 초대", "%s님이 [%s] 배틀에 초대했습니다.", GUARANTEED),
  BATTLE_RESULT("배틀 결과", "[%s] 배틀이 종료되었어요. 결과를 확인해보세요!", GUARANTEED),
  BATTLE_RESULT_WITH_WINNER("배틀 결과", "[%s] 배틀 종료! 1위는 [%s] 🏆 결과를 확인해보세요!", GUARANTEED),
  BATTLE_DEADLINE_OWNER_D7("마감 7일 전", "내 배틀 [%s] 종료까지 7일 남았어요. 공유하고 참여를 늘려보세요.", GUARANTEED),

  // 시스템 - 운영자 발신. 유실은 운영 사고
  SYSTEM("시스템 알림", "%s", GUARANTEED);

  private final String title;
  private final String contentTemplate;
  private final DeliveryGuarantee guarantee;

  public String formatContent(String... args) {
    return String.format(contentTemplate, (Object[]) args);
  }

  public boolean isGuaranteed() {
    return guarantee == GUARANTEED;
  }
}
