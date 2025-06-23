package com.example.toycontent.app.category.contoller.dto;

import com.example.toycontent.app.category.domain.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.toycontent.app.category.domain.Category.convertKeywordsToString;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor
    @Builder
    public static class Create {

        @Schema(title = "카테고리명", example = "스포츠용품")
        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
        private String categoryName;

        @Schema(title = "카테고리 설명", example = "각종 운동기구 및 스포츠 용품")
        @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
        private String description;

        @Schema(title = "카테고리 코드", example = "SPORTS")
        @Size(max = 50, message = "카테고리 코드는 50자 이하여야 합니다.")
        private String categoryCode;

        @Schema(title = "키워드 목록", example = "[\"운동\", \"헬스\", \"피트니스\"]")
        @Size(max = 10, message = "키워드는 최대 10개까지 입력 가능합니다.")
        private List<@NotBlank @Size(max = 20, message = "키워드는 20자 이하여야 합니다.") String> keywords;


        public Category toEntity(Integer lastOrder) {
            return Category.builder()
                    .name(this.categoryName)
                    .description(this.description)
                    .keywords(convertKeywordsToString(this.keywords))
                    .sortOrder(lastOrder)
                    .isActive(true)
                    .build();
        }

    }


    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static class Update {

        @Schema(title = "카테고리명", example = "스포츠용품")
        @NotBlank(message = "카테고리명은 필수입니다.")
        @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다.")
        private String categoryName;

        @Schema(title = "카테고리 설명", example = "각종 운동기구 및 스포츠 용품")
        @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
        private String description;

        @Schema(title = "키워드 목록", example = "[\"운동\", \"헬스\", \"피트니스\"]")
        @Size(max = 10, message = "키워드는 최대 10개까지 입력 가능합니다.")
        private List<@NotBlank @Size(max = 20, message = "키워드는 20자 이하여야 합니다.") String> keywords;

        @Schema(title = "정렬 순서", example = "1")
        @NotNull(message = "정렬 순서는 필수입니다.")
        @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.")
        private Integer sortOrder;
    }



}
