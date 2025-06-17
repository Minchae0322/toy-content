package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.category.contoller.dto.CategoryResponse;
import com.example.toycontent.app.common.enumuration.ProductStatus;
import com.example.toycontent.app.common.enumuration.Unit;
import com.example.toycontent.app.file.controller.dto.AttachmentFileResponse;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneMouthResponse {

    @Schema(title = "게시글 기본 키 ID")
    private Long oneMouthId;

    @Schema(title = "게시글 제목")
    private String title;

    @Schema(title = "사용자 설정 수량 값")
    private String quantity;

    @Schema(title = "유닛 (개, 입, 그램, 커스텀)")
    private Unit unit;

    @Schema(title = "가격")
    private Long price;

    @Schema(title = "판매 상태 (판매중, 품절, 예약중, 판매중단)")
    private ProductStatus productStatus;

    @Schema(title = "제품 상세 설명")
    private String description;

    @Schema(title = "판매자 아이디")
    private Long sellerId;

    @Schema(title = "카테고리")
    private CategoryResponse category;

    @Schema(title = "판매자 거래 위치")
    private String location;

    @Schema(title = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(title = "수정 시간")
    private LocalDateTime updatedAt;

    @Schema(title = "제품 유형 (예: rental, sale)")
    private String productType;

    @Schema(title = "조회수")
    private Integer hits;

    @Schema(title = "관심수")
    private Integer favoritesCount;

    @Schema(title = "첨부파일 리스트")
    private List<AttachmentFileResponse> oneMouthAttachmentFiles;

    public OneMouthResponse(
            Long oneMouthId,        // oneMouth.id
            String title,           // oneMouth.title
            String quantity,        // oneMouth.quantity
            Unit unit,              // oneMouth.unit
            Long price,             // oneMouth.price
            ProductStatus productStatus, // oneMouth.productStatus
            String description,     // oneMouth.description
            Long sellerId,          // oneMouth.sellerId
            Long categoryId,        // oneMouth.category.id - 단순 ID만
            String location,        // oneMouth.location
            LocalDateTime createdAt, // oneMouth.createdAt
            LocalDateTime updatedAt, // oneMouth.updatedAt
            String productType,     // oneMouth.productType
            Integer hits            // oneMouth.hits
    ) {
        this.oneMouthId = oneMouthId;
        this.title = title;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.productStatus = productStatus;
        this.description = description;
        this.sellerId = sellerId;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.productType = productType;
        this.hits = hits;

        // CategoryResponse는 나중에 별도로 설정
        if (categoryId != null) {
            this.category = CategoryResponse.builder()
                    .categoryId(categoryId)
                    .build();
        }
    }
    public static OneMouthResponse from(OneMouth oneMouth) {
        return OneMouthResponse.builder()
                .oneMouthId(oneMouth.getId())
                .title(oneMouth.getTitle())
                .quantity(oneMouth.getQuantity())
                .unit(oneMouth.getUnit())
                .price(oneMouth.getPrice())
                .productStatus(oneMouth.getProductStatus())
                .description(oneMouth.getDescription())
                .sellerId(oneMouth.getSellerId())
                .category(CategoryResponse.from(oneMouth.getCategory()))
                .oneMouthAttachmentFiles(oneMouth.getOneMouthAttachmentFiles().stream()
                        .map(oneMouthAttachmentFile ->
                                AttachmentFileResponse.from(oneMouthAttachmentFile.getAttachmentFile()))
                        .toList())
                .build();
    }

    @Data
    @Builder
    public static class Get {

    }
}
