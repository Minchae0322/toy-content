package com.example.toycontent.app.common.enumuration;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationChannel {

  IN_APP("IN_APP", "인앱 알림"),
  PUSH("PUSH", "푸시 알림"),
  EMAIL("EMAIL", "이메일"),
  SMS("SMS", "문자");

  private final String code;
  private final String description;

  public static NotificationChannel of(String code) {
    for (NotificationChannel channel : values()) {
      if (channel.code.equals(code)) {
        return channel;
      }
    }
    throw new IllegalArgumentException("Invalid NotificationChannel code: " + code);
  }
}
