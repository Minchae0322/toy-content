package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SaleType {

  ONEMOUTH("한입만", "소량 판매"),
  NORMAL("일반 판매", "일반적인 판매"),
  GROUP_BUY("공동구매", "여러 명이 함께 구매"),
  PROXY("대리 구매", "대신 구매해주는 서비스");

  private final String displayName;
  private final String description;
}
