package com.example.toycontent.app.file.domain.dto;

import com.example.toycontent.app.Product.domain.Product;
import com.example.toycontent.app.Product.domain.ProductAttachmentFile;
import com.example.toycontent.app.Product.domain.ProductReview;
import com.example.toycontent.app.Product.domain.ProductReviewAttachmentFile;
import com.example.toycontent.app.oneMouth.domain.SalePostAttachmentFile;
import com.example.toycontent.app.oneMouth.domain.SalePost;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

public abstract class AttachmentFileRequest {

  /**
   * 첨부파일 정보 DTO
   */
  @Data
  @Builder
  @Schema(description = "첨부파일 정보")
  public static class AttachmentInfo {

    @NotNull(message = "파일 ID는 필수입니다.")
    @Schema(description = "파일 ID", example = "123")
    private Long fileId;

    @NotBlank(message = "파일 URL은 필수입니다.")
    @Schema(description = "파일 저장 경로", example = "/uploads/2025/10/01/20251001230231_6577ef42.png")
    private String storedPath;

    @Schema(description = "원본 파일명", example = "product-image.png")
    private String originName;

    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long fileSize;

    @Schema(description = "파일 타입", example = "image/png")
    private String contentType;

    @Schema(description = "파일 설명", example = "제품 대표 이미지")
    private String fileExplain;


    public ProductAttachmentFile toEntity(Product product, int ord, boolean isPrimary) {
      return ProductAttachmentFile.builder()
          .product(product)
          .attachFileId(this.fileId)
          .fileUrl(this.storedPath)
          .orgFileNm(this.originName)
          .fileSize(this.fileSize)
          .contentType(this.contentType)
          .fileExplain(this.fileExplain)
          .sortOrder(ord)
          .isPrimary(isPrimary)
          .build();
    }

    public ProductReviewAttachmentFile toEntity(ProductReview productReview, int ord) {
      return ProductReviewAttachmentFile.builder()
          .productReview(productReview)
          .attachFileId(this.fileId)
          .fileUrl(this.storedPath)
          .orgFileNm(this.originName)
          .fileSize(this.fileSize)
          .contentType(this.contentType)
          .fileExplain(this.fileExplain)
          .sortOrder(ord)
          .build();
    }

    public SalePostAttachmentFile toEntity(SalePost salePost, int ord, boolean isPrimary) {
      return SalePostAttachmentFile.builder()
          .salePost(salePost)
          .attachFileId(this.fileId)
          .fileUrl(this.storedPath)
          .orgFileNm(this.originName)
          .fileSize(this.fileSize)
          .contentType(this.contentType)
          .fileExplain(this.fileExplain)
          .sortOrder(ord)
          .isPrimary(isPrimary)
          .build();
    }
  }



}
