package com.example.toycontent.app.notification.hotcontent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotContentCandidate {

  private HotContentType type;
  private Long contentId;
  private String displayName;
  private Double hotScore;
}
