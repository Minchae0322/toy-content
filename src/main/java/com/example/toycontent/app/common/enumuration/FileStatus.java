package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FileStatus {

  ACTIVE("ACTIVE", "활성"),
  DELETED("DELETED", "삭제됨"),
  TEMP("TEMP", "임시")
  ;

  private String title;
  private String description;

  public static FileStatus ofCode(String title) {
    return values()[Integer.parseInt(title.toUpperCase())];
  }
}
