package com.example.toycontent.app.common.enumuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

  // 댓글
  FEED_COMMENT("새 댓글", "%s님이 [%s] 피드에 댓글을 남겼습니다."),
  BATTLE_ITEM_COMMENT("새 댓글", "%s님이 [%s] 배틀의 [%s] 아이템에 댓글을 남겼습니다."),

  // 좋아요
  FEED_LIKE("좋아요", "%s님이 [%s] 피드에 좋아요를 눌렀습니다."),
  BATTLE_ITEM_LIKE("좋아요", "%s님이 [%s] 배틀의 [%s] 아이템에 좋아요를 눌렀습니다."),

  // 소셜
  FOLLOW("새 팔로우", "%s님이 회원님을 팔로우했습니다."),

  // 배틀
  BATTLE_INVITE("배틀 초대", "%s님이 [%s] 배틀에 초대했습니다."),
  BATTLE_RESULT("배틀 결과", "[%s] 배틀 결과가 나왔습니다."),

  // 시스템
  SYSTEM("시스템 알림", "%s");

  private final String title;
  private final String contentTemplate;

  public String formatContent(String... args) {
    return String.format(contentTemplate, (Object[]) args);
  }
}

