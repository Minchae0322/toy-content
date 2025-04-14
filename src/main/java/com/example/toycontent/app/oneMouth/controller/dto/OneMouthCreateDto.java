package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.Unit;
import com.example.toycontent.app.oneMouth.domain.OneMouthPost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "게시글 생성 요청 DTO")
public class OneMouthCreateDto {
    @Schema(title = "게시글 제목", description = "제품을 설명하는 제목", example = "중고 PS5 팝니다")
    private String title;

    @Schema(title = "게시글 내용", description = "상세 설명 및 거래 조건 등 게시글 본문", example = "직거래만 가능하며, 박스 있음")
    private String content;

    @Schema(title = "사용자 입력 수량", description = "1/2개, 3개 등 커스텀 수량 입력 값", example = "1/2")
    private String quantity;

    @Schema(title = "단위", description = "제품 단위 (예: EA_1, KG_1, ONE_SET 등)", example = "EA_1")
    private Unit unit;

    @Schema(title = "제품 이름", description = "판매 또는 거래하려는 물품의 이름", example = "플레이스테이션5")
    private String name;

    @Schema(title = "제품 상세 설명", description = "물품에 대한 상세한 설명", example = "2021년형 디스크 버전, 상태 양호")
    private String description;

    @Schema(title = "판매자 ID", description = "판매자의 고유 ID", example = "101")
    private Long sellerId;

    @Schema(title = "카테고리 ID", description = "등록할 카테고리의 ID", example = "3")
    private Long categoryId;

    @Schema(title = "남은 수량", description = "현재 판매 가능한 수량", example = "2")
    private Integer availableQuantity;

    @Schema(title = "거래 위치", description = "직거래 혹은 배송을 위한 지역 정보", example = "서울시 강남구")
    private String location;

    @Schema(title = "제품 유형", description = "판매 또는 대여 구분 (예: sale, rental)", example = "sale")
    private String productType;

    public OneMouthPost toEntity(Category category) {
        LocalDateTime now = LocalDateTime.now();

        return OneMouthPost.builder()
                .title(this.title)
                .content(this.content)
                .quantity(this.quantity)
                .unit(this.unit)
                .name(this.name)
                .description(this.description)
                .sellerId(this.sellerId)
                .category(category)
                .availableQuantity(this.availableQuantity)
                .location(this.location)
                .productType(this.productType)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
