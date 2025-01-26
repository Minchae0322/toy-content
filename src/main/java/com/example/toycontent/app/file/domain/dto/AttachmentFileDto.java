package com.example.toycontent.app.file.domain.dto;

import com.example.toycontent.app.common.enumuration.FileCode;
import com.example.toycontent.app.file.domain.AttachmentFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Builder
public class AttachmentFileDto {

    @Schema(description = "첨부 파일 ID", example = "1")
    private Long id;

    @Schema(description = "파일 코드", example = "UPLOAD_SUCCESS")
    private FileCode fileCode;

    @Schema(description = "원본 파일 이름", example = "example.jpg")
    private String orgFileNm;


    @Schema(description = "저장된 파일 이름", example = "12345.jpg")
    private String storeFileNm;


    @Schema(description = "파일 URL", example = "https://example.com/files/12345.jpg")
    private String fileUrl;

    @Schema(description = "파일 크기 (바이트 단위)", example = "1024")
    private Long fileSize;

    @Schema(description = "파일 설명", example = "샘플 파일 설명입니다.")
    private String fileExplain;

    public static AttachmentFileDto of(AttachmentFile attachmentFile) {
        return AttachmentFileDto.builder()
                .id(attachmentFile.getId())
                .fileCode(attachmentFile.getFileCode())
                .orgFileNm(attachmentFile.getOrgFileNm())
                .storeFileNm(attachmentFile.getStoreFileNm())
                .fileUrl(attachmentFile.getFileUrl())
                .fileSize(attachmentFile.getFileSize())
                .fileExplain(attachmentFile.getFileExplain())
                .build();
    }

}
