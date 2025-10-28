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
public enum ResultVisibility {
  REALTIME("REALTIME", "실시간 공개"),
  AFTER_END("AFTER_END", "종료 후 공개");

  private String title;
  private String description;

  public static ResultVisibility getResultVisibility(String title) {
    return Arrays.stream(values())
        .filter(visibility -> visibility.getTitle().equalsIgnoreCase(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid ResultVisibility: " + title));
  }
}
