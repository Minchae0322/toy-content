package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.Unit;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Schema(description = "게시글 생성 요청 DTO")
public class OneMouthCreateDto {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Schema(title = "게시글 제목", description = "제품을 설명하는 제목", example = "중고 PS5 팝니다")
    private String title;

    @Schema(title = "게시글 내용", description = "상세 설명 및 거래 조건 등 게시글 본문", example = "직거래만 가능하며, 박스 있음")
    private String content;

    @Schema(title = "사용자 입력 수량", description = "사용자 수량 입력 값", example = "한, 1")
    private String quantity;

    @Schema(title = "단위", description = "제품 단위 (예: 개 : EA, 입: MOUTH ,그램: GRAM, 커스텀: CUSTOM 등)", example = "EA")
    private Unit unit;

    @Schema(title = "판매자 ID", description = "판매자의 고유 ID", example = "101")
    private Long sellerId;

    @Schema(title = "카테고리 ID", description = "등록할 카테고리의 ID", example = "3")
    private Long categoryId;

    @Schema(title = "거래 위치", description = "직거래 혹은 배송을 위한 지역 정보", example = "서울시 강남구")
    private String location;

    @Schema(title = "제품 유형", description = "판매 또는 대여 구분 (예: SALE, RENTAL)", example = "sale")
    private String productType;

    private List<Long> photos = new ArrayList<>();

    public OneMouth toEntity(Category category) {

        LocalDateTime now = LocalDateTime.now();

        return OneMouth.builder()
                .title(this.title)
                .content(this.content)
                .quantity(this.quantity)
                .unit(this.unit)
                .sellerId(this.sellerId)
                .category(category)
                .location(this.location)
                .productType(this.productType)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }


}
