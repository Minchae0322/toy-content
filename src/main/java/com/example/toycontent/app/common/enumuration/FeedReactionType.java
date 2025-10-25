package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FeedReactionType {
  LIKE("LIKE", "좋아요"),
  HOT("HOT", "핫해요");

  private final String code;
  private final String description;
}
