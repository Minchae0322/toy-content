package com.example.toycontent.app.file.controller.dto;

import com.example.toycontent.app.file.domain.AttachmentFile;
import com.example.toycontent.app.oneMouth.domain.OneMouthAttachmentFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttachmentFileResponse {
    @Schema(description = "첨부 파일 ID", example = "123")
    private Long id;

    @Schema(description = "파일 코드 (예: THUMBNAIL, DOCUMENT 등)", example = "THUMBNAIL")
    private String fileCode;

    @Schema(description = "원본 파일명", example = "sample.jpg")
    private String orgFileNm;

    @Schema(description = "파일 접근 URL", example = "https://cdn.example.com/files/sample.jpg")
    private String fileUrl;

    @Schema(description = "파일 크기 (byte)", example = "204800")
    private Long fileSize;

    @Schema(description = "파일 설명", example = "대표 썸네일 이미지입니다.")
    private String fileExplain;

    public static AttachmentFileResponse from(AttachmentFile entity) {
        return AttachmentFileResponse.builder()
                .id(entity.getId())
                .fileCode(entity.getFileCode() != null ? entity.getFileCode().name() : null)
                .orgFileNm(entity.getOrgFileNm())
                .fileUrl(entity.getFileUrl())
                .fileSize(entity.getFileSize())
                .fileExplain(entity.getFileExplain())
                .build();
    }

    public static AttachmentFileResponse from(OneMouthAttachmentFile entity) {
        return AttachmentFileResponse.builder()
            .id(entity.getId())
            .orgFileNm(entity.getOrgFileNm())
            .fileUrl(entity.getFileUrl())
            .fileSize(entity.getFileSize())
            .fileExplain(entity.getFileExplain())
            .build();
    }
}
