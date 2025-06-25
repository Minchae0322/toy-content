package com.example.toycontent.app.category.contoller.dto;

import com.example.toycontent.app.category.domain.Category;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder

public class CategoryResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class List {
        @Schema(title = "카테고리 아이디")
        private Long categoryId;

        @Schema(title = "카테고리명")
        private String categoryName;

        @Schema(title = "카테고리 설명")
        private String description;

        @Schema(title = "정렬 순서")
        private Integer sortOrder;

        @Schema(title = "활성화 여부")
        private Boolean isActive;

        @Schema(title = "키워드")
        private String keywords;

        @Schema(title = "생성일시")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(title = "수정일시")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        public static List from(Category category) {
            return List.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .description(category.getDescription())
                    .sortOrder(category.getSortOrder())
                    .isActive(category.getIsActive())
                    .keywords(category.getKeywords())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        @Schema(title = "카테고리 아이디")
        private Long categoryId;

        @Schema(title = "카테고리명")
        private String categoryName;

        @Schema(title = "카테고리 설명")
        private String description;

        @Schema(title = "카테고리 코드")
        private String categoryCode;

        @Schema(title = "키워드 목록")
        private java.util.List<String> keywords;

        @Schema(title = "아이콘 URL")
        private String iconUrl;

        @Schema(title = "색상 코드")
        private String colorCode;

        @Schema(title = "정렬 순서")
        private Integer sortOrder;

        @Schema(title = "활성화 여부")
        private Boolean isActive;

        @Schema(title = "생성일시")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(title = "수정일시")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        public static Detail from(Category category) {
            return Detail.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .description(category.getDescription())
                    .sortOrder(category.getSortOrder())
                    .isActive(category.getIsActive())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdatedAt())
                    .build();
        }
    }



}
