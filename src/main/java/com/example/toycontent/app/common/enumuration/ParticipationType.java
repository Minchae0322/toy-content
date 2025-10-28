package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ParticipationType {
  ITEM_REGISTRATION("ITEM_REGISTRATION", "아이템 등록"),
  VOTE("VOTE", "투표");

  private String title;
  private String description;

  public static ParticipationType getParticipationType(String title) {
    return Arrays.stream(values())
        .filter(type -> type.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid ParticipationType: " + title));
  }
}