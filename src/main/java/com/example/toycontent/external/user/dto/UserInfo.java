package com.example.toycontent.external.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

  @Schema(description = "사용자 아이디")
  private Long userId;

  @Schema(description = "사용자 닉네임")
  private String nickname;

  @Schema(description = "사용자 이메일")
  private String email;

  @Schema(description = "프로필 이미지 파일")
  private ExternalAttachmentFileDto profileImageFile;

  @Schema(description = "유저 권한")
  private String role;

  @Schema(description = "계정 활성화 여부")
  private boolean activated;

  @Schema(description = "FCM 토큰")
  private String fcmToken;
}
