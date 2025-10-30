package com.example.toycontent.app.common.enumuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이템 추가 권한 타입
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ItemAddPermissionType {
  CREATOR_ONLY("CREATOR_ONLY", "생성자만 추가 가능", "배틀 생성자만 아이템을 추가할 수 있습니다"),
  PUBLIC_FREE("PUBLIC_FREE", "누구나 추가 가능", "모든 사용자가 자유롭게 아이템을 추가할 수 있습니다"),
  PUBLIC_APPROVAL("PUBLIC_APPROVAL", "승인 후 추가 가능", "아이템 등록 후 생성자의 승인을 받아야 합니다");

  private String code;
  private String title;
  private String description;

  public static ItemAddPermissionType getType(String code) {
    return Arrays.stream(values())
        .filter(type -> type.getCode().equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid ItemAddPermissionType: " + code));
  }

  /**
   * 생성자만 추가 가능한지 확인
   */
  public boolean isCreatorOnly() {
    return this == CREATOR_ONLY;
  }

  /**
   * 누구나 자유롭게 추가 가능한지 확인
   */
  public boolean isPublicFree() {
    return this == PUBLIC_FREE;
  }

  /**
   * 승인 필요 여부 확인
   */
  public boolean requiresApproval() {
    return this == PUBLIC_APPROVAL;
  }
}
