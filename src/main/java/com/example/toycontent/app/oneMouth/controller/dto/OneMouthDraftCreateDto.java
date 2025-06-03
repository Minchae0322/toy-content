package com.example.toycontent.app.oneMouth.controller.dto;

import com.example.toycontent.app.category.domain.Category;
import com.example.toycontent.app.common.enumuration.Unit;
import com.example.toycontent.app.oneMouth.domain.OneMouth;
import com.example.toycontent.app.oneMouth.domain.OneMouthDraft;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Schema(description = "게시글 임시 저장 요청 DTO")
public class OneMouthDraftCreateDto {

    @Schema(title = "게시글 제목", description = "제품을 설명하는 제목", example = "중고 PS5 팝니다")
    private String title;

    @Schema(title = "게시글 내용", description = "상세 설명 및 거래 조건 등 게시글 본문", example = "직거래만 가능하며, 박스 있음")
    private String content;

    @Schema(title = "사용자 입력 수량", description = "1/2개, 3개 등 커스텀 수량 입력 값", example = "1/2")
    private String quantity;

    @Schema(title = "단위", description = "제품 단위 (예: EA_1, KG_1, ONE_SET 등)", example = "EA_1")
    private Unit unit;

    @Schema(title = "카테고리 ID", description = "등록할 카테고리의 ID", example = "3")
    private Long categoryId;

    @Schema(title = "거래 위치", description = "직거래 혹은 배송을 위한 지역 정보", example = "서울시 강남구")
    private String location;

    @Schema(title = "판매자 ID", description = "판매자의 고유 ID", example = "101")
    private Long sellerId;

    private List<Long> photos = new ArrayList<>();

    public OneMouthDraft toEntity(Category category) {
        LocalDateTime now = LocalDateTime.now();

        return OneMouthDraft.builder()
                .title(this.title)
                .content(this.content)
                .quantity(this.quantity)
                .unit(this.unit)
                .sellerId(this.sellerId)
                .category(category)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
