package com.example.toycontent.app.category.contoller.dto;

import com.example.toycontent.app.category.domain.Category;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder

public class CategoryResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListView {
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

        @Schema(title = "해당 카테고리로 작성된 글 개수")
        private Long contentCount;

        @Schema(title = "생성일시")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(title = "수정일시")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;

        @Schema(title = "자식 카테고리 목록")
        private List<ListView> children;

        public static ListView from(Category category) {
            return ListView.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .keywords(category.getKeywords())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .children(category.getChildren().stream()
                    .map(ListView::from)
                    .toList())
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
        private List<String> keywords;

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

        @Schema(title = "자식 카테고리 목록")
        private List<ListView> children;

        public static Detail from(Category category) {
            return Detail.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .description(category.getDescription())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .children(category.getChildren().stream()
                    .map(ListView::from)
                    .toList())
                .build();
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리 상세 정보")
    public static class SubCategoryDetail {

        @Schema(description = "대분류 카테고리 ID", example = "1")
        private Long categoryId;

        @Schema(description = "소분류 카테고리 ID", example = "10")
        private Long subCategoryId;

        @Schema(description = "대분류 카테고리 명", example = "전자제품")
        private String categoryName;

        @Schema(description = "소분류 카테고리 명", example = "노트북")
        private String subCategoryName;

        public static SubCategoryDetail from(Category category) {
            return SubCategoryDetail.builder()
                .categoryId(Optional.ofNullable(category.getParent())
                    .map(Category::getId)
                    .orElse(null))
                .categoryName(Optional.ofNullable(category.getParent())
                    .map(Category::getName)
                    .orElse(null))
                .subCategoryId(category.getId())
                .subCategoryName(category.getName())
                .build();
        }

    }


}
