package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StickerType {

  EMOJI("이모지", "이모지 스티커"),
  PHOTO_TAG("사진택", "이미지를 포함한 사진 스티커"),
  PRESET("프리셋", "앱 제공 기본 스티커");

  private final String displayName;
  private final String description;
}
