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
public enum VoteType {
  SINGLE("SINGLE", "1인 1표"),
  MULTIPLE("MULTIPLE", "1인 3표"),
  ;

  private String title;
  private String description;

  public static VoteType getVoteType(String title) {
    return Arrays.stream(values())
        .filter(type -> type.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid VoteType: " + title));
  }
}
