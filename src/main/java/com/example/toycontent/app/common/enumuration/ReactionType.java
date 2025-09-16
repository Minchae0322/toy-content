package com.example.toycontent.app.common.enumuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionType {

  LIKE("LIKE", "좋아요", "사용자가 제품을 좋아함을 표시"),
  BOOKMARK("BOOKMARK", "북마크", "나중에 다시 보기 위해 저장"),
  INTEREST("INTEREST", "관심상품", "구매나 체험에 관심이 있음을 표시"),
  SHARE("SHARE", "공유", "다른 사용자에게 공유"),
  WISH("WISH", "위시리스트", "구매하고 싶은 상품으로 등록");

  private final String code;
  private final String displayName;
  private final String description;

  public static ReactionType fromCode(String code) {
    for (ReactionType type : ReactionType.values()) {
      if (type.getCode().equals(code)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown ReactionType code: " + code);
  }

  public boolean isPositiveReaction() {
    return this == LIKE || this == BOOKMARK || this == INTEREST || this == WISH;
  }

  public boolean isEngagementAction() {
    return this == SHARE;
  }
}
