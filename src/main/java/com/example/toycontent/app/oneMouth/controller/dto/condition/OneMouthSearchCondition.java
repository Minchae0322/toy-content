package com.example.toycontent.app.oneMouth.controller.dto.condition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "한입만 게시물 검색 조건")

public class OneMouthSearchCondition {

    @Schema(description = "검색 키워드", example = "딸기")
    private String keyword;

    @Schema(description = "조회 시작일 (yyyy-MM-dd)", example = "2024-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "조회 종료일 (yyyy-MM-dd)", example = "2024-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
