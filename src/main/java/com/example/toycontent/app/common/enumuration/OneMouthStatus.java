package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OneMouthStatus {

  ON_SALE("ON_SALE", "판매중"),
  OUT_OF_STOCK("OUT_OF_STOCK", "품절"),
  RESERVED("RESERVED", "예약중"),
  DISCONTINUED("DISCONTINUED", "판매중단"),
  ;

  private String title;
  private String description;

  public static OneMouthStatus getOneMouthStatus(String title) {
    for (OneMouthStatus status : values()) {
      if (status.title.equals(title)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid SaleStatus title: " + title);
  }
}
