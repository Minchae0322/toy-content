package com.example.toycontent.app.common.enumuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

  // 댓글
  FEED_COMMENT("새 댓글", "%s님이 [%s] 피드에 댓글을 남겼습니다."),
  BATTLE_ITEM_COMMENT("새 댓글", "%s님이 [%s] 배틀의 [%s] 아이템에 댓글을 남겼습니다."),

  // 배틀 아이템 추가
  BATTLE_ITEM_ADDED("새 아이템", "%s님이 [%s] 배틀에 [%s] 아이템을 추가했어요."),
  BATTLE_ITEM_ADDED_BATCH("새 아이템", "%s님이 [%s] 배틀에 [%s] 외 %s개 아이템을 추가했어요."),
  BATTLE_ITEM_APPROVAL_REQUEST("승인 요청", "%s님이 [%s] 배틀에 [%s] 아이템 등록을 요청했어요. 승인해주세요."),
  BATTLE_ITEM_APPROVAL_REQUEST_BATCH("승인 요청", "%s님이 [%s] 배틀에 [%s] 외 %s개 아이템 등록을 요청했어요. 승인해주세요."),

  // 좋아요
  FEED_LIKE("좋아요", "%s님이 [%s] 피드에 좋아요를 눌렀습니다."),
  BATTLE_ITEM_LIKE("좋아요", "%s님이 [%s] 배틀의 [%s] 아이템에 좋아요를 눌렀습니다."),

  // 소셜
  FOLLOW("새 팔로우", "%s님이 회원님을 팔로우했습니다."),

  // 배틀
  BATTLE_INVITE("배틀 초대", "%s님이 [%s] 배틀에 초대했습니다."),
  BATTLE_RESULT("배틀 결과", "[%s] 배틀이 종료되었어요. 결과를 확인해보세요!"),
  BATTLE_DEADLINE_OWNER_D7("마감 7일 전", "내 배틀 [%s] 종료까지 7일 남았어요. 공유하고 참여를 늘려보세요."),

  // 시스템
  SYSTEM("시스템 알림", "%s");

  private final String title;
  private final String contentTemplate;

  public String formatContent(String... args) {
    return String.format(contentTemplate, (Object[]) args);
  }
}

