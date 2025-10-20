package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum GroupBuyType {

  OPEN("OPEN", "공개"),
  PRIVATE("PRIVATE", "비공개"),
  ;

  private String title;
  private String description;
}
