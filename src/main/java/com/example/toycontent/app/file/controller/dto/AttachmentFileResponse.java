package com.example.toycontent.app.file.controller.dto;

import com.example.toycontent.app.product.domain.ProductAttachmentFile;
import com.example.toycontent.app.product.domain.ProductReviewAttachmentFile;
import com.example.toycontent.app.feed.domain.FeedAttachmentFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor  // 이것만 추가하면 됨
@AllArgsConstructor
public class AttachmentFileResponse {

    @Schema(description = "첨부 파일 ID", example = "1")
    private Long id;

    @Schema(description = "원본 파일 이름", example = "example.jpg")
    private String orgFileNm;

    @Schema(description = "파일 URL", example = "https://example.com/files/12345.jpg")
    private String fileUrl;

    @Schema(description = "파일 크기 (바이트 단위)", example = "1024")
    private Long fileSize;

    @Schema(description = "파일 설명", example = "샘플 파일 설명입니다.")
    private String fileExplain;

    @Schema(description = "content_type")
    private String contentType;

    public static AttachmentFileResponse of(ProductAttachmentFile attachmentFile) {
        return AttachmentFileResponse.builder()
                .id(attachmentFile.getId())
                .orgFileNm(attachmentFile.getOrgFileNm())
                .fileUrl(attachmentFile.getFileUrl())
                .fileSize(attachmentFile.getFileSize())
                .fileExplain(attachmentFile.getFileExplain())
                .build();
    }

    public static AttachmentFileResponse of(ProductReviewAttachmentFile attachmentFile) {
        return AttachmentFileResponse.builder()
            .id(attachmentFile.getId())
            .orgFileNm(attachmentFile.getOrgFileNm())
            .fileUrl(attachmentFile.getFileUrl())
            .fileSize(attachmentFile.getFileSize())
            .fileExplain(attachmentFile.getFileExplain())
            .build();
    }

    public static AttachmentFileResponse of(FeedAttachmentFile attachmentFile) {
        return AttachmentFileResponse.builder()
            .id(attachmentFile.getId())
            .orgFileNm(attachmentFile.getOrgFileNm())
            .fileUrl(attachmentFile.getFileUrl())
            .fileSize(attachmentFile.getFileSize())
            .fileExplain(attachmentFile.getFileExplain())
            .build();
    }

}
