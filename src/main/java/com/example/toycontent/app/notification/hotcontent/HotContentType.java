package com.example.toycontent.app.notification.hotcontent;

import com.example.toycontent.app.common.enumuration.NotificationReferenceType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HotContentType {

  FEED("피드", NotificationReferenceType.FEED, "/feed/"),
  BATTLE("배틀", NotificationReferenceType.BATTLE, "/battle/");

  private final String label;
  private final NotificationReferenceType referenceType;
  private final String actionUrlPrefix;

  public String buildActionUrl(Long contentId) {
    return actionUrlPrefix + contentId;
  }
}
