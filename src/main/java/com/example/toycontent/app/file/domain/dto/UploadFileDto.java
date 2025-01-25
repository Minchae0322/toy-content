package com.example.toycontent.app.file.domain.dto;

import com.example.toycontent.app.file.domain.AttachmentFile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Builder
public class UploadFileDto {

    @Schema(description = "첨부 파일 아이디")
    private Long id;

    @Schema(description = "원본 파일 명")
    private String orgFileNm;

    @Schema(description = "저장 파일 명")
    private String storeFileNm;

    public static UploadFileDto of(AttachmentFile attachmentFile) {
        return UploadFileDto.builder()
                .id(attachmentFile.getId())
                .orgFileNm(attachmentFile.getOrgFileNm())
                .storeFileNm(attachmentFile.getStoreFileNm())
                .build();
    }

}
