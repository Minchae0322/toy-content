package com.example.toycontent.external.user.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalAttachmentFileDto {

  @Schema(description = "첨부 파일 아이디")
  private Long id;

  @Schema(description = "원본 파일 명")
  private String orgFileNm;

  @Schema(description = "저장 파일 명")
  private String storeFileNm;

  @Schema(description = "파일 url")
  private String fileUrl;

  @Schema(description = "파일 사이즈")
  private Long fileSize;

  @Schema(description = "파일 설명")
  private String fileExplain;
}


