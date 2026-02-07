package com.example.toycontent.app.category.contoller.dto;

import com.example.toycontent.app.common.enumuration.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public abstract class CategorySearchCondition {

    private String keyword;
    private Boolean isActive;

    @Getter
    @Setter
    public static class PageSearch extends CategorySearchCondition {
    }

    @Getter
    @Setter
    public static class PopularSearch extends CategorySearchCondition {

        @Schema(description = "콘텐츠 타입 (필수)", example = "BATTLE")
        @NotNull
        private ContentType type;

        @Schema(description = "조회 시작일 (필수)", example = "2026-01-01")
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;

        @Schema(description = "조회 종료일 (필수)", example = "2026-02-07")
        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
    }
}
