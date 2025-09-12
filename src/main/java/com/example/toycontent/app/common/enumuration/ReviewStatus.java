package com.example.toycontent.app.common.enumuration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStatus {

  ACTIVE("활성", "정상적으로 노출되는 리뷰"),
  DELETED("삭제", "사용자가 삭제한 리뷰"),
  HIDDEN("숨김", "신고나 관리자에 의해 숨겨진 리뷰"),
  PENDING("대기", "검토 대기 중인 리뷰");

  private final String displayName;
  private final String description;
}
