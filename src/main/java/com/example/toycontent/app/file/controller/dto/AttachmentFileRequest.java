package com.example.toycontent.app.file.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

public abstract class AttachmentFileRequest {

  @Data
  @Builder
  public static class SimpleDto {

    @Schema(description = "첨부 파일 아이디", example = "12345")
    private Long attachFileId;

    @Schema(description = "파일 URL", example = "https://s3.amazonaws.com/my-bucket/files/2024/01/uuid-123.jpg")
    private String fileUrl;

    @Schema(description = "원본 파일명", example = "맛있는음식사진.jpg")
    private String orgFileNm;

    @Schema(description = "파일 크기 (바이트)", example = "2048576")
    private Long fileSize;

    @Schema(description = "파일 MIME 타입", example = "image/jpeg",
        allowableValues = {"image/jpeg", "image/png", "image/gif", "application/pdf", "text/plain", "video/mp4"})
    private String contentType;

    @Schema(description = "정렬 순서", example = "1")
    private Integer sortOrder;

    @Schema(description = "파일 설명", example = "오늘 먹은 맛있는 음식 사진입니다")
    private String fileExplain;
  }

}
