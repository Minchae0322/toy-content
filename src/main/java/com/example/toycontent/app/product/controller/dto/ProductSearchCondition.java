package com.example.toycontent.app.product.controller.dto;

import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "제품 검색 조건")
public class ProductSearchCondition {

  @Schema(description = "검색 키워드 (제품명, 브랜드)", example = "아메리카노")
  private String keyword;

  @Schema(description = "카테고리 ID", example = "1")
  private Long categoryId;

  @Schema(description = "브랜드명", example = "스타벅스")
  private String brand;

  @Schema(description = "제품 유형", example = "SALE")
  private String productType;

  @JsonIgnore
  @Schema(description = "제품 상태", example = "APPROVED", hidden = true)
  private ProductStatus status;


}
